package com.kampplus.hava.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.kampplus.hava.model.TemperatureUnit

private val LightColors = lightColorScheme(
    primary = Sky40,
    onPrimary = Neutral99,
    primaryContainer = Sky90,
    onPrimaryContainer = Sky10,
    secondary = Sun40,
    secondaryContainer = Sun90,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceContainer = Neutral95,
    error = Red40
)

private val DarkColors = darkColorScheme(
    primary = Sky80,
    onPrimary = Sky20,
    primaryContainer = Sky30,
    onPrimaryContainer = Sky90,
    secondary = Sun80,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    onSurfaceVariant = NeutralVariant90,
    error = Red80
)

/**
 * Sıcaklık biriminin ağaçtaki her bileşene taşınması.
 *
 * `CompositionLocal`, parametre olarak elden ele geçirmeden "ortamdaki" değeri okumaya yarar.
 * Birim ayarı değişince bunu okuyan her ekran kendiliğinden yeniden çizilir.
 */
val LocalTemperatureUnit = staticCompositionLocalOf { TemperatureUnit.CELSIUS }

/** Açık/koyu tema ile sıcaklık birimini sağlayan kök sarmalayıcı. */
@Composable
fun HavaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTemperatureUnit provides temperatureUnit) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = HavaTypography,
            content = content
        )
    }
}
