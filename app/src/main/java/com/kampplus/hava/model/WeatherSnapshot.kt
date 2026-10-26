package com.kampplus.hava.model

import java.time.LocalDateTime

/**
 * Bir yerin hava durumunun kısa özeti. Bildirim ve ana ekran widget'ı tüm tahmini değil
 * yalnızca bu özeti gösterir.
 */
data class WeatherSnapshot(
    val place: Place,
    val temperatureC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val maxC: Double?,
    val minC: Double?,
    val precipitationProbability: Int?,
    val updatedAt: LocalDateTime
) {
    val condition: WeatherCondition
        get() = WeatherCondition.fromWeatherCode(weatherCode)
}

fun Forecast.toSnapshot(place: Place): WeatherSnapshot {
    val today = daily.firstOrNull()
    return WeatherSnapshot(
        place = place,
        temperatureC = current.temperatureC,
        weatherCode = current.weatherCode,
        isDay = current.isDay,
        maxC = today?.maxTemperatureC,
        minC = today?.minTemperatureC,
        precipitationProbability = today?.precipitationProbability,
        updatedAt = current.observedAt
    )
}
