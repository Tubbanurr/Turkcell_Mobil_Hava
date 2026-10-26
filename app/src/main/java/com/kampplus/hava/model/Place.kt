package com.kampplus.hava.model

import java.util.Locale

/**
 * Hava durumu sorgulanabilen bir yer: arama sonucu, favori, geçmiş kaydı ya da
 * telefonun o anki konumu.
 *
 * [City] ağdan gelen ham şehir kaydıdır; uygulamanın geri kalanı yalnızca bu sade modeli bilir.
 *
 * @param isCurrentLocation true ise koordinat GPS'ten gelmiştir ("Konumum").
 */
data class Place(
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false
) {
    /** Aynı yeri tanımak için kullanılan anahtar (~1 km duyarlılık). Favori ve geçmişin birincil anahtarıdır. */
    val key: String
        get() = "%.2f,%.2f".format(Locale.ROOT, latitude, longitude)

    companion object {
        /** İlk açılışta gösterilen yer; kullanıcı ayarlardan ya da konumdan değiştirir. */
        val Default = Place(
            name = "Kocaeli",
            subtitle = "Kocaeli, Türkiye",
            latitude = 40.7654,
            longitude = 29.9408
        )
    }
}

fun City.toPlace() = Place(name = name, subtitle = subtitle, latitude = latitude, longitude = longitude)
