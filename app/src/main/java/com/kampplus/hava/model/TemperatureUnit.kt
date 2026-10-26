package com.kampplus.hava.model

import kotlin.math.roundToInt

/**
 * Sıcaklık birimi. Servisten ve modellerden her zaman Celsius gelir;
 * dönüşüm yalnızca ekrana yazarken yapılır (tek doğruluk kaynağı Celsius kalır).
 */
enum class TemperatureUnit(val symbol: String, val label: String) {
    CELSIUS("°C", "Celsius"),
    FAHRENHEIT("°F", "Fahrenheit");

    fun convert(celsius: Double): Double = when (this) {
        CELSIUS -> celsius
        FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
    }

    /** Tam sayıya yuvarlanmış derece: "21°". */
    fun format(celsius: Double): String = "${convert(celsius).roundToInt()}°"
}
