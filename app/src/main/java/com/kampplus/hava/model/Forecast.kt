package com.kampplus.hava.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Bir şehrin anlık durumu + önümüzdeki saatler ve günler.
 *
 * Tahmin ekranının veri modeli: bir şehre dokunulduğunda gösterilen bilgiler.
 * Zamanlar `timezone=auto` sayesinde şehrin **yerel** saatindedir.
 */
data class Forecast(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>
) {
    /** Şu andan itibaren (bulunulan saat dahil) en fazla [limit] saatlik tahmin. */
    fun upcomingHours(limit: Int = 24): List<HourlyForecast> {
        val start = current.observedAt.truncatedTo(ChronoUnit.HOURS)
        return hourly.filter { !it.time.isBefore(start) }.take(limit)
    }

    /** Verilen an gündüz mü? Gün doğumu/batımı varsa onlara, yoksa 06-20 aralığına bakar. */
    fun isDay(time: LocalDateTime): Boolean {
        val day = daily.firstOrNull { it.date == time.toLocalDate() }
        val sunrise = day?.sunrise
        val sunset = day?.sunset
        return if (sunrise != null && sunset != null) {
            !time.isBefore(sunrise) && time.isBefore(sunset)
        } else {
            time.hour in DAY_START_HOUR until DAY_END_HOUR
        }
    }

    private companion object {
        const val DAY_START_HOUR = 6
        const val DAY_END_HOUR = 20
    }
}

/**
 * Şu anki hava. Son üç alan servis tarafından her zaman gönderilmez,
 * bu yüzden nullable'dır ve ekranda yalnızca varsa gösterilir.
 */
data class CurrentWeather(
    val temperatureC: Double,
    val weatherCode: Int,
    val observedAt: LocalDateTime,
    val apparentTemperatureC: Double? = null,
    val humidityPercent: Int? = null,
    val windSpeedKmh: Double? = null,
    val isDay: Boolean = true,
    val pressureHpa: Double? = null
)

/** Tek bir saatlik tahmin satırı. */
data class HourlyForecast(
    val time: LocalDateTime,
    val temperatureC: Double,
    val weatherCode: Int,
    val precipitationProbability: Int? = null,
    val pressureHpa: Double? = null,
    val cloudCoverPercent: Int? = null,
    val windSpeedKmh: Double? = null
)

/** Tek bir günlük tahmin satırı: o günün en düşük ve en yüksek sıcaklığı. */
data class DailyForecast(
    val date: LocalDate,
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
    val weatherCode: Int,
    val precipitationProbability: Int? = null,
    val sunrise: LocalDateTime? = null,
    val sunset: LocalDateTime? = null,
    val uvIndexMax: Double? = null
)
