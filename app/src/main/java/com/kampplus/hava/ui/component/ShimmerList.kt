package com.kampplus.hava.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kampplus.hava.ui.theme.HavaTheme
import com.kampplus.hava.ui.weather.SkyBackground
import com.kampplus.hava.ui.weather.SkyPalette

/**
 * Yükleniyor durumunun görünümü.
 *
 * Shimmer, içerik gelmeden önce içeriğin **iskeletini** gösterir ve üzerinden
 * kayan bir ışıltı geçirir. Dönen bir çark yerine bunu tercih ederiz; çünkü
 * kullanıcı ne geleceğini önceden görür ve bekleme daha kısa hissedilir.
 */
@Composable
fun ShimmerList(modifier: Modifier = Modifier, itemCount: Int = 6) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Gerçek kartlarla aynı ölçülerde boş kartlar çizilir.
        repeat(itemCount) {
            GlassCard(modifier = Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBlock(brush, Modifier.size(44.dp), RoundedCornerShape(12.dp))
                    Column(
                        modifier = Modifier.padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerBlock(brush, Modifier.fillMaxWidth(0.8f).height(16.dp))
                        ShimmerBlock(brush, Modifier.fillMaxWidth(0.5f).height(12.dp))
                    }
                }
            }
        }
    }
}

/** Shimmer fırçasıyla boyanmış tek bir iskelet parçası. */
@Composable
fun ShimmerBlock(brush: Brush, modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(6.dp)) {
    Box(modifier = modifier.background(brush, shape))
}

/**
 * Soldan sağa kayan ışıltı efekti.
 *
 * `rememberInfiniteTransition` + `infiniteRepeatable`: bitmeyen bir animasyon kurar.
 * Değişen değer, gradient'in yatay konumudur; her karede biraz sağa kayar.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val translate by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = SHIMMER_DISTANCE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.10f)),
        start = Offset(translate - SHIMMER_WIDTH, 0f),
        end = Offset(translate, 0f)
    )
}

private const val SHIMMER_DISTANCE = 1_500f
private const val SHIMMER_WIDTH = 400f

@Preview
@Composable
private fun ShimmerListPreview() {
    HavaTheme {
        SkyBackground(SkyPalette.Calm) {
            ShimmerList()
        }
    }
}
