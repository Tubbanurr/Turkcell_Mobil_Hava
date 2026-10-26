package com.kampplus.hava.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Gökyüzü üzerinde yarı saydam "buzlu cam" kart rengi. */
val GlassColor = Color.White.copy(alpha = 0.16f)
val GlassBorder = Color.White.copy(alpha = 0.14f)
val GlassShape = RoundedCornerShape(24.dp)

/** Gökyüzü arka planı üstünde kullanılan ortak kart. Dokunulabilir hâli için [onClick] ver. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val body: @Composable () -> Unit = { Column(modifier = Modifier.padding(padding), content = content) }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = GlassShape,
            color = GlassColor,
            contentColor = Color.White,
            border = BorderStroke(1.dp, GlassBorder),
            content = body
        )
    } else {
        Surface(
            modifier = modifier,
            shape = GlassShape,
            color = GlassColor,
            contentColor = Color.White,
            border = BorderStroke(1.dp, GlassBorder),
            content = body
        )
    }
}
