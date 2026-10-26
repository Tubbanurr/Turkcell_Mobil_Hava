package com.kampplus.hava.ui.weather

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Hava durumuna göre renk geçişli, hareketli ekran arkaplanı.
 *
 * İçerik beyaz renkle çizilir (`LocalContentColor`): gökyüzü her zaman yeterince koyudur.
 * Palet değişince (örn. şehir değişince) renkler yumuşakça geçiş yapar.
 */
@Composable
fun SkyBackground(palette: SkyPalette, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val top by animateColorAsState(palette.top, tween(COLOR_TRANSITION_MS), label = "skyTop")
    val bottom by animateColorAsState(palette.bottom, tween(COLOR_TRANSITION_MS), label = "skyBottom")
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
    ) {
        SkyEffects(effect = palette.effect, glow = palette.glow)
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            content()
        }
    }
}

private data class Particle(val x: Float, val y: Float, val speed: Int, val size: Float, val phase: Float)

private fun particles(count: Int, maxSpeed: Int): List<Particle> {
    val random = Random(SEED)
    return List(count) {
        Particle(
            x = random.nextFloat(),
            y = random.nextFloat(),
            // Tam sayı hız: animasyon başa sararken sıçrama olmaz.
            speed = 1 + random.nextInt(maxSpeed),
            size = 0.6f + random.nextFloat() * 0.8f,
            phase = random.nextFloat()
        )
    }
}

@Composable
private fun SkyEffects(effect: SkyEffect, glow: Color?) {
    val duration = when (effect) {
        SkyEffect.RAIN -> RAIN_MS
        SkyEffect.SNOW -> SNOW_MS
        SkyEffect.STARS -> STARS_MS
        SkyEffect.CLOUDS -> CLOUDS_MS
        SkyEffect.NONE -> STARS_MS
    }
    val items = remember(effect) {
        when (effect) {
            SkyEffect.RAIN -> particles(RAIN_COUNT, 2)
            SkyEffect.SNOW -> particles(SNOW_COUNT, 2)
            SkyEffect.STARS -> particles(STAR_COUNT, 1)
            SkyEffect.CLOUDS -> particles(CLOUD_COUNT, 1)
            SkyEffect.NONE -> emptyList()
        }
    }
    val progress by rememberInfiniteTransition(label = "sky").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(duration, easing = LinearEasing)),
        label = "skyProgress"
    )
    Canvas(Modifier.fillMaxSize()) {
        // `progress` yalnızca çizim aşamasında okunur: her karede yeniden düzenleme (recomposition) olmaz.
        val t = progress
        glow?.let {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(it, Color.Transparent),
                    center = Offset(size.width * GLOW_X, size.height * GLOW_Y),
                    radius = GLOW_RADIUS.dp.toPx()
                ),
                radius = GLOW_RADIUS.dp.toPx(),
                center = Offset(size.width * GLOW_X, size.height * GLOW_Y)
            )
        }
        when (effect) {
            SkyEffect.RAIN -> items.forEach { p ->
                val y = ((p.y + t * p.speed) % 1f) * (size.height + 40.dp.toPx()) - 20.dp.toPx()
                val x = p.x * size.width
                drawLine(
                    color = Color.White.copy(alpha = 0.28f * p.size),
                    start = Offset(x, y),
                    end = Offset(x - 5.dp.toPx(), y + 16.dp.toPx() * p.size),
                    strokeWidth = 1.3.dp.toPx()
                )
            }
            SkyEffect.SNOW -> items.forEach { p ->
                val y = ((p.y + t * p.speed) % 1f) * (size.height + 20.dp.toPx()) - 10.dp.toPx()
                val sway = sin(2 * PI * (t * p.speed + p.phase)).toFloat() * 14.dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = 2.6.dp.toPx() * p.size,
                    center = Offset(p.x * size.width + sway, y)
                )
            }
            SkyEffect.STARS -> items.forEach { p ->
                val twinkle = 0.5f + 0.5f * sin(2 * PI * (t + p.phase)).toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f + 0.65f * twinkle),
                    radius = 1.6.dp.toPx() * p.size,
                    center = Offset(p.x * size.width, p.y * size.height * STAR_AREA)
                )
            }
            SkyEffect.CLOUDS -> items.forEachIndexed { index, p ->
                val radius = (150 + 60 * p.size).dp.toPx()
                val x = ((p.x + t) % 1f) * (size.width + 2 * radius) - radius
                val y = size.height * (0.08f + 0.16f * index)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(x, y),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
            SkyEffect.NONE -> Unit
        }
    }
}

private const val SEED = 7
private const val COLOR_TRANSITION_MS = 700
private const val RAIN_MS = 1_100
private const val SNOW_MS = 9_000
private const val STARS_MS = 4_000
private const val CLOUDS_MS = 90_000
private const val RAIN_COUNT = 70
private const val SNOW_COUNT = 45
private const val STAR_COUNT = 50
private const val CLOUD_COUNT = 4
private const val STAR_AREA = 0.55f
private const val GLOW_X = 0.82f
private const val GLOW_Y = 0.10f
private const val GLOW_RADIUS = 260
