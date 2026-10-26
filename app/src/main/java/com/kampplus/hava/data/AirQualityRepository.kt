package com.kampplus.hava.data

import com.kampplus.hava.model.AirQuality
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Hava kalitesine erişimin tek kapısı; tahmin ekranı Retrofit'i bilmez. */
interface AirQualityRepository {
    /** Veri yoksa `null` döner; ağ hatasında istisna fırlatır. */
    suspend fun getAirQuality(latitude: Double, longitude: Double): AirQuality?
}

class OpenMeteoAirQualityRepository(
    private val api: AirQualityApi = ApiClient.airQualityApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AirQualityRepository {

    override suspend fun getAirQuality(latitude: Double, longitude: Double): AirQuality? = withContext(ioDispatcher) {
        api.getAirQuality(latitude, longitude).toAirQuality()
    }
}
