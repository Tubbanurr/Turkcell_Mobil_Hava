package com.kampplus.hava.model

/** Kullanıcının kalıcı tercihleri (DataStore'da saklanır). */
data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val notificationsEnabled: Boolean = false,
    val homePlace: Place = Place.Default
)
