package com.kampplus.hava.ui.weather

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.kampplus.hava.model.WeatherCondition

/** Gökyüzünde çizilen hareketli efekt. */
enum class SkyEffect { NONE, CLOUDS, RAIN, SNOW, STARS }

/**
 * Bir hava durumunun renk ve efekt karşılığı: ekran arka planı bundan çizilir.
 *
 * @param glow Güneş ışıması gibi köşede parlayan renk; yoksa `null`.
 */
@Immutable
data class SkyPalette(val top: Color, val bottom: Color, val effect: SkyEffect, val glow: Color? = null) {

    /** Koyu temada tüm renkler siyaha doğru çekilir, efektler korunur. */
    fun dimmed(): SkyPalette = copy(
        top = lerp(top, Color.Black, DIM_FRACTION),
        bottom = lerp(bottom, Color.Black, DIM_FRACTION),
        glow = glow?.copy(alpha = glow.alpha * GLOW_DIM)
    )

    companion object {
        private const val DIM_FRACTION = 0.38f
        private const val GLOW_DIM = 0.6f

        /** Veri yokken (yükleniyor, hata, ayarlar) gösterilen sakin gökyüzü. */
        val Calm = SkyPalette(Color(0xFF2E6FC4), Color(0xFF6FA8E8), SkyEffect.CLOUDS)

        fun of(condition: WeatherCondition, isDay: Boolean, dark: Boolean = false): SkyPalette {
            val palette = base(condition, isDay)
            return if (dark) palette.dimmed() else palette
        }

        fun calm(dark: Boolean): SkyPalette = if (dark) Calm.dimmed() else Calm

        private fun base(condition: WeatherCondition, isDay: Boolean): SkyPalette = when (condition) {
            WeatherCondition.CLEAR ->
                if (isDay) {
                    SkyPalette(Color(0xFF2E86DE), Color(0xFF74B9FF), SkyEffect.NONE, Color(0x66FFE29A))
                } else {
                    SkyPalette(Color(0xFF0B1230), Color(0xFF2B3A67), SkyEffect.STARS)
                }
            WeatherCondition.MAINLY_CLEAR, WeatherCondition.PARTLY_CLOUDY ->
                if (isDay) {
                    SkyPalette(Color(0xFF4A90D9), Color(0xFF8FB8E8), SkyEffect.CLOUDS, Color(0x40FFE29A))
                } else {
                    SkyPalette(Color(0xFF16203D), Color(0xFF354566), SkyEffect.STARS)
                }
            WeatherCondition.OVERCAST ->
                if (isDay) {
                    SkyPalette(Color(0xFF6C7A89), Color(0xFF9AA8B5), SkyEffect.CLOUDS)
                } else {
                    SkyPalette(Color(0xFF1E2A44), Color(0xFF3C4A66), SkyEffect.CLOUDS)
                }
            WeatherCondition.FOG ->
                if (isDay) {
                    SkyPalette(Color(0xFF8D99A6), Color(0xFFB8C2CC), SkyEffect.CLOUDS)
                } else {
                    SkyPalette(Color(0xFF3A4452), Color(0xFF5A6572), SkyEffect.CLOUDS)
                }
            WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS ->
                SkyPalette(Color(0xFF34495E), Color(0xFF5D7C99), SkyEffect.RAIN)
            WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS ->
                SkyPalette(Color(0xFF7F9BB8), Color(0xFFC9D8E6), SkyEffect.SNOW)
            WeatherCondition.THUNDERSTORM ->
                SkyPalette(Color(0xFF1F2638), Color(0xFF4B4E6D), SkyEffect.RAIN)
            WeatherCondition.UNKNOWN -> Calm
        }
    }
}
