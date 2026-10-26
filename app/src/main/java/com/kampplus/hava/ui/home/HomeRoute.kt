package com.kampplus.hava.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.ui.forecast.PlaceForecastRoute

/**
 * "Bugün" sekmesi. Konum butonu önce çalışma anı iznini ister (Android 6+), izin varsa
 * [HomeViewModel.onLocateMe] ile GPS konumunu ana şehir yapar.
 */
@Composable
fun HomeRoute(
    darkTheme: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val context = LocalContext.current
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) viewModel.onLocateMe() else viewModel.onLocationPermissionDenied()
    }

    val onLocateClick = {
        val granted = LOCATION_PERMISSIONS.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) viewModel.onLocateMe() else permissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    PlaceForecastRoute(
        viewModel = viewModel,
        darkTheme = darkTheme,
        bottomPadding = bottomPadding,
        modifier = modifier,
        actions = { LocateButton(isLocating = isLocating, onClick = onLocateClick) }
    )
}

@Composable
private fun RowScope.LocateButton(isLocating: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = !isLocating) {
        if (isLocating) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = androidx.compose.ui.graphics.Color.White)
        } else {
            Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.action_locate))
        }
    }
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION
)
