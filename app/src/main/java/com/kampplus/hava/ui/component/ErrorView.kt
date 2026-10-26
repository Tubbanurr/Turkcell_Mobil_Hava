package com.kampplus.hava.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kampplus.hava.R
import com.kampplus.hava.ui.theme.HavaTheme
import com.kampplus.hava.ui.weather.SkyBackground
import com.kampplus.hava.ui.weather.SkyPalette

/**
 * Hata durumunun görünümü.
 *
 * Iki kural:
 * 1. Kullanıcıya teknik mesaj gösterilmez. "UnknownHostException" değil,
 *    "İnternet bağlantını kontrol et." yazarız. Teknik ayrıntı Logcat'e gider.
 * 2. Hata ekranı her zaman bir çıkış yolu sunar: "Tekrar dene".
 *
 * @param onRetry null verilirse buton çizilmez (tekrar denemenin anlamsız olduğu durumlar).
 */
@Composable
fun ErrorView(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = LocalContentColor.current.copy(alpha = 0.85f)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        onRetry?.let {
            Button(
                onClick = it,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.25f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Text(
                    text = stringResource(R.string.action_retry),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ErrorViewPreview() {
    HavaTheme {
        SkyBackground(SkyPalette.Calm) {
            ErrorView(message = "İnternet bağlantını kontrol et.", onRetry = {})
        }
    }
}
