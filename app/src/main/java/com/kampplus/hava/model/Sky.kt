package com.kampplus.hava.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.cos

/** Ayın evresi: görünür aydınlık oranı (0..1) ve adı. */
data class MoonPhase(val illumination: Double, val name: String, val emoji: String)

private const val SYNODIC_MONTH_DAYS = 29.530588853

/** 6 Ocak 2000'deki yeni aydan bu yana geçen günlerle ayın evresi (ortalama hesap, ±yarım gün). */
fun moonPhaseOn(date: LocalDate): MoonPhase {
    val daysSinceNewMoon = date.toEpochDay() - NEW_MOON_EPOCH_DAY - NEW_MOON_OFFSET_DAYS
    val age = ((daysSinceNewMoon % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS
    val illumination = (1 - cos(2 * PI * age / SYNODIC_MONTH_DAYS)) / 2
    val (name, emoji) = when {
        age < 1.85 -> "Yeni ay" to "🌑"
        age < 5.54 -> "Büyüyen hilal" to "🌒"
        age < 9.22 -> "İlk dördün" to "🌓"
        age < 12.91 -> "Büyüyen şişkin ay" to "🌔"
        age < 16.61 -> "Dolunay" to "🌕"
        age < 20.30 -> "Küçülen şişkin ay" to "🌖"
        age < 23.99 -> "Son dördün" to "🌗"
        age < 27.68 -> "Küçülen hilal" to "🌘"
        else -> "Yeni ay" to "🌑"
    }
    return MoonPhase(illumination, name, emoji)
}

// 2000-01-06 18:14 UTC; öğle saatine göre ölçüldüğü için 0.5 çıkarılıp 0.76 eklenir.
private val NEW_MOON_EPOCH_DAY = LocalDate.of(2000, 1, 6).toEpochDay()
private const val NEW_MOON_OFFSET_DAYS = 0.26

/** Her yıl tekrarlayan başlıca meteor yağmurları (tepe noktası tarihleri yaklaşıktır). */
enum class MeteorShower(val label: String, val month: Int, val day: Int) {
    QUADRANTIDS("Quadrantid", 1, 3),
    LYRIDS("Lirid", 4, 22),
    ETA_AQUARIDS("Eta Akuarid", 5, 6),
    PERSEIDS("Perseid", 8, 12),
    ORIONIDS("Orionid", 10, 21),
    LEONIDS("Leonid", 11, 17),
    GEMINIDS("Geminid", 12, 14)
}

data class UpcomingShower(val shower: MeteorShower, val daysUntil: Long)

/** [within] gün içinde tepe yapacak en yakın meteor yağmuru; yoksa `null`. */
fun nextMeteorShower(from: LocalDate, within: Long = 30): UpcomingShower? = MeteorShower.entries
    .map { shower ->
        var peak = LocalDate.of(from.year, shower.month, shower.day)
        if (peak.isBefore(from)) peak = peak.plusYears(1)
        UpcomingShower(shower, ChronoUnit.DAYS.between(from, peak))
    }
    .filter { it.daysUntil <= within }
    .minByOrNull { it.daysUntil }

/** Bu gece yıldız gözlemi için tahmin: 0–100 puan ve kısa yorum. */
data class Stargazing(val score: Int, val avgCloudPercent: Int?, val moon: MoonPhase) {
    val verdict: String
        get() = when {
            score >= 75 -> "Mükemmel"
            score >= 55 -> "İyi"
            score >= 35 -> "Orta"
            else -> "Kötü"
        }
}

/**
 * Gece 21:00 – 03:00 arasındaki ortalama bulutluluk ve ay ışığıyla gözlem puanı.
 * Bulutluluk verisi yoksa `null`. Işık kirliliği verisi bu servisten gelmez; şehir dışı daha iyidir.
 */
fun Forecast.stargazingTonight(): Stargazing? {
    val observed: LocalDateTime = current.observedAt
    val nightDate = if (observed.hour < NIGHT_END_HOUR) observed.toLocalDate().minusDays(1) else observed.toLocalDate()
    val start = nightDate.atTime(NIGHT_START_HOUR, 0)
    val end = nightDate.plusDays(1).atTime(NIGHT_END_HOUR, 0)
    val clouds = hourly.filter { !it.time.isBefore(start) && !it.time.isAfter(end) }.mapNotNull { it.cloudCoverPercent }
    if (clouds.isEmpty()) return null
    val avgCloud = clouds.average()
    val moon = moonPhaseOn(nightDate)
    val score = ((100 - avgCloud) * CLOUD_WEIGHT + (1 - moon.illumination) * 100 * MOON_WEIGHT).toInt().coerceIn(0, 100)
    return Stargazing(score, avgCloud.toInt(), moon)
}

private const val NIGHT_START_HOUR = 21
private const val NIGHT_END_HOUR = 3
private const val CLOUD_WEIGHT = 0.75
private const val MOON_WEIGHT = 0.25
