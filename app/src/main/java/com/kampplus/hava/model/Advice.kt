package com.kampplus.hava.model

import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Kullanıcıya gösterilen tek bir pratik öneri: "☂️ Şemsiye al". */
data class Advice(val emoji: String, val title: String, val detail: String)

/**
 * Tahminden günlük önerileri üretir ("ne giymeli, yanıma ne almalı?").
 *
 * Saf bir fonksiyondur: ağ, ekran, saat yok; aynı tahmin her zaman aynı öneriyi verir.
 * Bu yüzden birim testle kolayca doğrulanır. Uyarılar (yağmur, fırtına) listenin başına,
 * giyim önerisi sona yakın yerleşir.
 */
fun Forecast.buildAdvice(): List<Advice> {
    val warnings = buildList {
        rainAdvice()?.let(::add)
        snowAdvice()?.let(::add)
        stormAdvice()?.let(::add)
        windAdvice()?.let(::add)
        sunAdvice()?.let(::add)
        swingAdvice()?.let(::add)
        humidityAdvice()?.let(::add)
        pressureAdvice()?.let(::add)
    }
    return warnings + clothingAdvice()
}

private val HourFormat = DateTimeFormatter.ofPattern("HH:mm")
private const val LOOKAHEAD_HOURS = 12
private const val RAIN_PERCENT = 50
private const val WINDY_KMH = 40.0
private const val UV_HIGH = 6.0
private const val BIG_SWING_C = 12.0
private const val PRESSURE_WINDOW_HOURS = 12
private const val PRESSURE_SWING_HPA = 6.0

private fun Forecast.nextHours() = upcomingHours(LOOKAHEAD_HOURS)

private fun Forecast.rainAdvice(): Advice? {
    val first = nextHours().firstOrNull { hour ->
        val rainy = WeatherCondition.fromWeatherCode(hour.weatherCode) in RAINY
        rainy || (hour.precipitationProbability ?: 0) >= RAIN_PERCENT
    } ?: return null
    val percent = first.precipitationProbability?.let { " (%$it)" }.orEmpty()
    return Advice("☂️", "Şemsiye al", "Saat ${first.time.format(HourFormat)} civarında yağış bekleniyor$percent.")
}

private fun Forecast.snowAdvice(): Advice? {
    val snowy = nextHours().any { WeatherCondition.fromWeatherCode(it.weatherCode) in SNOWY } ||
        WeatherCondition.fromWeatherCode(current.weatherCode) in SNOWY
    return if (snowy) Advice("🧤", "Kar var", "Yollar kayganlaşabilir; bot, eldiven ve bere iyi olur.") else null
}

private fun Forecast.stormAdvice(): Advice? {
    val storm = nextHours().any { WeatherCondition.fromWeatherCode(it.weatherCode) == WeatherCondition.THUNDERSTORM } ||
        WeatherCondition.fromWeatherCode(current.weatherCode) == WeatherCondition.THUNDERSTORM
    return if (storm) Advice("⚡", "Fırtına uyarısı", "Gök gürültülü sağanak var; açık alanda ve ağaç altında durma.") else null
}

private fun Forecast.windAdvice(): Advice? {
    val wind = current.windSpeedKmh ?: return null
    return if (wind >= WINDY_KMH) {
        Advice("🌬️", "Rüzgâr sert", "${wind.roundToInt()} km/sa esiyor; rüzgârlık al, şapkana dikkat.")
    } else {
        null
    }
}

private fun Forecast.sunAdvice(): Advice? {
    val uv = daily.firstOrNull()?.uvIndexMax ?: return null
    return if (uv >= UV_HIGH && current.isDay) {
        Advice("🕶️", "Güneş kremi sür", "UV indeksi ${uv.roundToInt()}; gözlük ve krem öğlen saatlerinde şart.")
    } else {
        null
    }
}

private fun Forecast.swingAdvice(): Advice? {
    val today = daily.firstOrNull() ?: return null
    val swing = today.maxTemperatureC - today.minTemperatureC
    return if (swing >= BIG_SWING_C) {
        Advice("🧅", "Katmanlı giyin", "Gün içinde sıcaklık ${swing.roundToInt()}° oynıyor; soyunup giyebileceğin bir kat al.")
    } else {
        null
    }
}

private fun Forecast.humidityAdvice(): Advice? {
    val humidity = current.humidityPercent ?: return null
    return if (humidity >= 85 && current.temperatureC >= 25) {
        Advice("💦", "Nem yüksek", "Hava bunaltıcı olabilir; bol su iç, ince ve nefes alan giysi seç.")
    } else {
        null
    }
}

/**
 * Basınç hızla değişirse migren ve eklem ağrısı (romatizma) tetiklenebilir.
 * Önümüzdeki 24 saatte, 12 saat içindeki en büyük düşüş ya da yükseliş bakılır.
 */
private fun Forecast.pressureAdvice(): Advice? {
    val values = upcomingHours(PRESSURE_WINDOW_HOURS * 2).mapNotNull { it.pressureHpa }
    if (values.size < 2) return null
    var biggest = 0.0
    for (i in values.indices) {
        for (j in i + 1..minOf(i + PRESSURE_WINDOW_HOURS, values.lastIndex)) {
            val delta = values[j] - values[i]
            if (kotlin.math.abs(delta) > kotlin.math.abs(biggest)) biggest = delta
        }
    }
    if (kotlin.math.abs(biggest) < PRESSURE_SWING_HPA) return null
    val direction = if (biggest < 0) "düşüyor" else "yükseliyor"
    return Advice(
        "🩺",
        "Basınç değişimi",
        "Basınç ${kotlin.math.abs(biggest).roundToInt()} hPa $direction; migren veya eklem ağrısı olanlar dikkatli olsun."
    )
}

/** Hissedilen sıcaklığa göre ne giyileceği. Hissedilen yoksa gerçek sıcaklık kullanılır. */
private fun Forecast.clothingAdvice(): Advice {
    val felt = current.apparentTemperatureC ?: current.temperatureC
    return when {
        felt <= 0 -> Advice("🧥", "Kalın mont giy", "Hissedilen ${felt.roundToInt()}°; mont, bere ve eldiven al.")
        felt <= 10 -> Advice("🧥", "Mont giy", "Hissedilen ${felt.roundToInt()}°; kalın bir üst iyi gider.")
        felt <= 17 -> Advice("🧣", "Ceket veya hırka al", "Hissedilen ${felt.roundToInt()}°; serin, ince bir ceket yeter.")
        felt <= 25 -> Advice("👕", "Hafif giyin", "Hissedilen ${felt.roundToInt()}°; tişört ve ince pantolon rahat eder.")
        felt <= 31 -> Advice("🩳", "İnce giyin", "Hissedilen ${felt.roundToInt()}°; açık renk, nefes alan kumaş seç.")
        else -> Advice("🥵", "Çok sıcak", "Hissedilen ${felt.roundToInt()}°; gölgede kal, bol su iç.")
    }
}

private val RAINY = setOf(
    WeatherCondition.DRIZZLE,
    WeatherCondition.RAIN,
    WeatherCondition.RAIN_SHOWERS,
    WeatherCondition.THUNDERSTORM
)
private val SNOWY = setOf(WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS)
