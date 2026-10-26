package com.kampplus.hava.data

import com.kampplus.hava.model.Advice
import com.kampplus.hava.model.TemperatureUnit
import com.kampplus.hava.model.WeatherSnapshot
import com.kampplus.hava.model.buildAdvice
import com.kampplus.hava.model.toSnapshot
import kotlinx.coroutines.flow.first

/** Bildirim ve widget'ın ihtiyacı olan şey: özet + hangi birimle gösterileceği. */
data class SyncResult(
    val snapshot: WeatherSnapshot,
    val unit: TemperatureUnit,
    val advice: List<Advice> = emptyList()
)

/**
 * "Ana şehrin güncel özetini getir" işi. Arka plan işçisi ve widget aynı mantığı kullanır;
 * ekranlardan bağımsızdır, bu yüzden tek başına test edilebilir.
 */
class WeatherSyncer(
    private val settingsRepository: SettingsRepository,
    private val weatherRepository: WeatherRepository
) {
    suspend fun sync(): SyncResult {
        val settings = settingsRepository.settings.first()
        val place = settings.homePlace
        val forecast = weatherRepository.getForecast(place.latitude, place.longitude)
        return SyncResult(forecast.toSnapshot(place), settings.temperatureUnit, forecast.buildAdvice())
    }
}
