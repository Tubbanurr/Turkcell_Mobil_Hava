package com.kampplus.hava.ui.forecast

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TurkishLocale = Locale.forLanguageTag("tr-TR")
private val HourFormatter = DateTimeFormatter.ofPattern("HH:mm", TurkishLocale)

internal fun LocalDateTime.hourLabel(): String = format(HourFormatter)

internal fun LocalDate.dayLabel(): String = dayOfWeek.getDisplayName(TextStyle.FULL, TurkishLocale)

/** UV indeksinin gündelik karşılığı (WHO sınıflaması). */
internal fun uvLabel(uv: Double): String = when {
    uv < 3 -> "Düşük"
    uv < 6 -> "Orta"
    uv < 8 -> "Yüksek"
    uv < 11 -> "Çok yüksek"
    else -> "Aşırı"
}

/** "Bugün", "Yarın" ya da gün adı: aktivite planlayıcıda kısa ve doğal görünür. */
internal fun LocalDate.relativeDayLabel(today: LocalDate): String = when (this) {
    today -> "Bugün"
    today.plusDays(1) -> "Yarın"
    else -> dayLabel()
}
