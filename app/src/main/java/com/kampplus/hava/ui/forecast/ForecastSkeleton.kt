package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kampplus.hava.ui.component.GlassCard
import com.kampplus.hava.ui.component.ShimmerBlock
import com.kampplus.hava.ui.component.rememberShimmerBrush

/** Tahmin gelene kadar gerçek ekranın iskeletini gösterir (hero + iki kart). */
@Composable
fun ForecastSkeleton(modifier: Modifier = Modifier) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBlock(brush, Modifier.fillMaxWidth(0.45f).height(28.dp))
        Spacer(Modifier.height(4.dp))
        ShimmerBlock(brush, Modifier.size(84.dp), RoundedCornerShape(42.dp))
        ShimmerBlock(brush, Modifier.fillMaxWidth(0.4f).height(72.dp), RoundedCornerShape(16.dp))
        ShimmerBlock(brush, Modifier.fillMaxWidth(0.3f).height(18.dp))
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(5) { ShimmerBlock(brush, Modifier.size(width = 48.dp, height = 96.dp), RoundedCornerShape(14.dp)) }
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(5) { ShimmerBlock(brush, Modifier.fillMaxWidth().height(22.dp)) }
            }
        }
    }
}
