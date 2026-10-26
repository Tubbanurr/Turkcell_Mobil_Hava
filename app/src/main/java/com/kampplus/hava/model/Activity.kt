package com.kampplus.hava.model

import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs

/** Hava durumuna göre en uygun zamanı aradığımız dış mekan aktiviteleri. */
enum class Activity(val label: String, val emoji: String) {
    RUNNING("Koşu", "🏃"),
    CAMPING("Kamp", "⛺"),
    BARBECUE("Mangal", "🍖"),
    PHOTOGRAPHY("Fotoğraf", "📷"),
    CAR_WASH("Araç yıkama", "🚗")
}

/** Önerilen tek bir zaman dilimi: ne zaman, kaç puan, o saatte hava nasıl. */
data class ActivitySlot(
    val time: LocalDateTime,
    val score: Int,
    val temperatureC: Double,
    val precipitationProbability: Int?
) {
    val verdict: String
        get() = when {
            score >= GREAT -> "Harika"
            score >= GOOD -> "İyi"
            score >= OKAY -> "Fena değil"
            else -> "Zayıf"
        }

    private companion object {
        const val GREAT = 80
        const val GOOD = 60
        const val OKAY = 40
    }
}

/**
 * [activity] için önümüzdeki günlerin en iyi saatlerini bulur (her gün için en fazla bir tane).
 *
 * Her saate 0–100 arası puan verilir: sıcaklığın konforu, yağmur ihtimali, rüzgâr ve
 * aktiviteye özgü koşullar (fotoğrafta gün doğumu/batımı, araç yıkamada sonraki 24 saat
 * yağmursuz olması gibi). Saf fonksiyon: aynı tahmin aynı sonucu verir.
 */
fun Forecast.planActivity(activity: Activity, limit: Int = 3): List<ActivitySlot> {
    val hours = upcomingHours(Int.MAX_VALUE)
    return hours.mapIndexedNotNull { index, hour ->
        val score = scoreHour(activity, hour, hours, index) ?: return@mapIndexedNotNull null
        ActivitySlot(hour.time, score, hour.temperatureC, hour.precipitationProbability)
    }
        .groupBy { it.time.toLocalDate() }
        .values
        .map { day -> day.maxBy { it.score } }
        .sortedByDescending { it.score }
        .take(limit)
}

private fun Forecast.scoreHour(activity: Activity, hour: HourlyForecast, all: List<HourlyForecast>, index: Int): Int? {
    val h = hour.time.hour
    val rain = rainRisk(hour)
    val wind = hour.windSpeedKmh ?: 0.0
    val windScore = (100 - (wind - CALM_WIND_KMH).coerceAtLeast(0.0) * WIND_PENALTY_PER_KMH).coerceIn(0.0, 100.0)
    val raw = when (activity) {
        Activity.RUNNING -> {
            if (h !in 6..21 || !isDay(hour.time)) return null
            comfort(hour.temperatureC, 8.0, 18.0) * 0.5 + (100 - rain) * 0.4 + windScore * 0.1
        }
        Activity.BARBECUE -> {
            if (h !in 16..21) return null
            comfort(hour.temperatureC, 18.0, 30.0) * 0.4 + (100 - rain) * 0.4 + windScore * 0.2
        }
        Activity.CAMPING -> {
            if (h !in 17..21) return null
            val nightRain = all.drop(index).take(CAMP_LOOKAHEAD_HOURS).maxOfOrNull(::rainRisk) ?: rain
            comfort(hour.temperatureC, 12.0, 24.0) * 0.35 + (100 - nightRain) * 0.5 + windScore * 0.15
        }
        Activity.PHOTOGRAPHY -> {
            val golden = goldenHourScore(hour.time)
            if (golden <= 0) return null
            val sky = hour.cloudCoverPercent?.let { 100 - abs(it - IDEAL_CLOUD_PERCENT) * 1.6 } ?: 60.0
            golden * 0.5 + sky.coerceIn(0.0, 100.0) * 0.3 + (100 - rain) * 0.2
        }
        Activity.CAR_WASH -> {
            if (h !in 9..18) return null
            val upcomingRain = all.drop(index).take(CAR_WASH_LOOKAHEAD_HOURS).maxOfOrNull(::rainRisk) ?: rain
            (100 - upcomingRain) * 0.85 + comfort(hour.temperatureC, 5.0, 35.0) * 0.15
        }
    }
    return raw.toInt().coerceIn(0, 100)
}

/** Yağış ihtimali; ihtimal verisi yoksa hava koduna bakılır. */
private fun rainRisk(hour: HourlyForecast): Int {
    val byCode = when (WeatherCondition.fromWeatherCode(hour.weatherCode)) {
        WeatherCondition.THUNDERSTORM, WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS,
        WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS -> 90
        WeatherCondition.DRIZZLE -> 60
        else -> 0
    }
    return maxOf(byCode, hour.precipitationProbability ?: 0)
}

/** İdeal aralıkta 100; aralığın dışında her derece için puan düşer. */
private fun comfort(temperature: Double, idealLow: Double, idealHigh: Double): Double {
    val off = when {
        temperature < idealLow -> idealLow - temperature
        temperature > idealHigh -> temperature - idealHigh
        else -> 0.0
    }
    return (100 - off * COMFORT_PENALTY_PER_DEGREE).coerceIn(0.0, 100.0)
}

/** Gün doğumu/batımına yakınlık: ±1 saat 100, ±2 saat 60, daha uzak 0. */
private fun Forecast.goldenHourScore(time: LocalDateTime): Double {
    val day = daily.firstOrNull { it.date == time.toLocalDate() } ?: return 0.0
    val distance = listOfNotNull(day.sunrise, day.sunset)
        .minOfOrNull { abs(Duration.between(it, time).toMinutes()) } ?: return 0.0
    return when {
        distance <= 60 -> 100.0
        distance <= 120 -> 60.0
        else -> 0.0
    }
}

private const val COMFORT_PENALTY_PER_DEGREE = 6.0
private const val CALM_WIND_KMH = 15.0
private const val WIND_PENALTY_PER_KMH = 3.0
private const val IDEAL_CLOUD_PERCENT = 40
private const val CAMP_LOOKAHEAD_HOURS = 12
private const val CAR_WASH_LOOKAHEAD_HOURS = 24
