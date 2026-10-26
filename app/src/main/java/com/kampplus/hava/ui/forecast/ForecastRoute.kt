package com.kampplus.hava.ui.forecast

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.theme.LocalTemperatureUnit

/**
 * Tahmin ekranının bağlayıcı katmanı: ViewModel'in state'ini toplar, paylaşım ve bilgi
 * mesajı (Snackbar) gibi Android'e özgü işleri üstlenir; çizimi [ForecastScreen]'e bırakır.
 *
 * Hem detay hem ana sekme bunu kullanır; aralarındaki fark slotlarla ([navigationIcon], [actions]) verilir.
 */
@Composable
fun PlaceForecastRoute(
    viewModel: PlaceForecastViewModel,
    darkTheme: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val unit = LocalTemperatureUnit.current

    // Mesaj gelince bir kez göster, sonra ViewModel'den temizle.
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    ForecastScreen(
        uiState = uiState,
        onRetry = viewModel::onRetry,
        onRefresh = viewModel::onRefresh,
        onToggleFavorite = viewModel::onToggleFavorite,
        onShare = {
            val place = uiState.place
            val forecast = uiState.forecast
            if (place != null && forecast is LoadState.Success) {
                context.shareText(buildShareText(place, forecast.data, unit))
            }
        },
        modifier = modifier,
        darkTheme = darkTheme,
        bottomPadding = bottomPadding,
        snackbarHostState = snackbarHostState,
        navigationIcon = navigationIcon,
        actions = actions
    )
}

/** Şehir detay ekranı: geri düğmesi + "ana şehir yap". */
@Composable
fun ForecastRoute(
    onBack: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    viewModel: ForecastViewModel = viewModel(factory = ForecastViewModel.Factory)
) {
    PlaceForecastRoute(
        viewModel = viewModel,
        darkTheme = darkTheme,
        bottomPadding = 0.dp,
        modifier = modifier,
        navigationIcon = { BackButton(onBack) },
        actions = { SetHomeButton(onClick = viewModel::onSetAsHome) }
    )
}

@Composable
private fun SetHomeButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.action_set_home))
    }
}
