package com.kampplus.hava.model

/**
 * Hava kalitesi, Avrupa Hava Kalitesi İndeksi'ne (EAQI) göre.
 *
 * İndeks ne kadar yüksekse hava o kadar kirlidir; [level] sayıyı insan diline çevirir.
 */
data class AirQuality(
    val europeanAqi: Int,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val dust: Double? = null,
    val pollen: List<PollenReading> = emptyList()
) {
    val level: AirQualityLevel
        get() = AirQualityLevel.fromAqi(europeanAqi)
}

enum class AirQualityLevel(val label: String, val emoji: String, val advice: String) {
    VERY_GOOD("Çok iyi", "🟢", "Temiz hava; dışarıda vakit geçirmek için ideal."),
    GOOD("İyi", "🟢", "Hava kalitesi iyi, açık hava aktivitelerine uygun."),
    MODERATE("Orta", "🟡", "Hassas kişiler uzun süreli yoğun egzersizi kısaltmalı."),
    POOR("Kötü", "🟠", "Astım ve kalp hastaları dışarıda uzun kalmamalı."),
    VERY_POOR("Çok kötü", "🔴", "Dışarıda yoğun efor yapma; mümkünse maske tak."),
    EXTREMELY_POOR("Tehlikeli", "🟣", "Mümkünse dışarı çıkma, pencereleri kapalı tut.");

    companion object {
        fun fromAqi(aqi: Int): AirQualityLevel = when {
            aqi <= 20 -> VERY_GOOD
            aqi <= 40 -> GOOD
            aqi <= 60 -> MODERATE
            aqi <= 80 -> POOR
            aqi <= 100 -> VERY_POOR
            else -> EXTREMELY_POOR
        }
    }
}

/** Tek bir polen türünün anlık yoğunluğu (tanecik/m³). Servis yalnızca Avrupa için veri verir. */
data class PollenReading(val name: String, val emoji: String, val grains: Double) {
    val level: String
        get() = when {
            grains < 1 -> "Yok"
            grains < 15 -> "Düşük"
            grains < 50 -> "Orta"
            grains < 150 -> "Yüksek"
            else -> "Çok yüksek"
        }
}
