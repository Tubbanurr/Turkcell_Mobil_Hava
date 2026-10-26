package com.kampplus.hava.data

import com.google.gson.annotations.SerializedName
import com.kampplus.hava.model.AirQuality
import com.kampplus.hava.model.PollenReading
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Hava kalitesi servisi (`air-quality-api.open-meteo.com`). Anahtar istemez.
 *
 * Örnek: `GET /v1/air-quality?latitude=39.92&longitude=32.85&current=european_aqi,pm10,pm2_5`
 */
interface AirQualityApi {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("timezone") timezone: String = "auto"
    ): AirQualityResponse

    companion object {
        const val CURRENT_FIELDS =
            "european_aqi,pm10,pm2_5,dust,alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen"
    }
}

data class AirQualityResponse(val current: AirQualityCurrent? = null)

data class AirQualityCurrent(
    @SerializedName("european_aqi") val europeanAqi: Double? = null,
    @SerializedName("pm10") val pm10: Double? = null,
    @SerializedName("pm2_5") val pm25: Double? = null,
    @SerializedName("dust") val dust: Double? = null,
    @SerializedName("alder_pollen") val alder: Double? = null,
    @SerializedName("birch_pollen") val birch: Double? = null,
    @SerializedName("grass_pollen") val grass: Double? = null,
    @SerializedName("mugwort_pollen") val mugwort: Double? = null,
    @SerializedName("olive_pollen") val olive: Double? = null,
    @SerializedName("ragweed_pollen") val ragweed: Double? = null
)

/** Servis bazı bölgelerde veri vermez; bu durumda `null` döner ve kart gizlenir. */
fun AirQualityResponse.toAirQuality(): AirQuality? {
    val aqi = current?.europeanAqi ?: return null
    val pollen = listOfNotNull(
        current.grass?.let { PollenReading("Çim", "🌾", it) },
        current.birch?.let { PollenReading("Huş", "🌳", it) },
        current.alder?.let { PollenReading("Kızılağaç", "🌲", it) },
        current.olive?.let { PollenReading("Zeytin", "🫒", it) },
        current.mugwort?.let { PollenReading("Pelin otu", "🌿", it) },
        current.ragweed?.let { PollenReading("Ambrozya", "🌼", it) }
    )
    return AirQuality(
        europeanAqi = aqi.toInt(),
        pm25 = current.pm25,
        pm10 = current.pm10,
        dust = current.dust,
        pollen = pollen
    )
}
