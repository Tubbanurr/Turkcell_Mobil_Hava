package com.kampplus.hava.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.TemperatureUnit
import com.kampplus.hava.model.ThemeMode
import com.kampplus.hava.model.UserSettings
import com.kampplus.hava.ui.component.GlassCard
import com.kampplus.hava.ui.theme.HavaTheme
import com.kampplus.hava.ui.weather.SkyBackground
import com.kampplus.hava.ui.weather.SkyPalette
import kotlinx.coroutines.launch

/**
 * Ayarlar sekmesinin bağlayıcı katmanı. Bildirim izni (Android 13+) burada, çalışma anında istenir.
 */
@Composable
fun SettingsRoute(
    darkTheme: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    // İzin sonucu gelene kadar "bekleyen" işi saklarız; izin verilirse çalıştırırız.
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingAction?.invoke()
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.settings_permission_denied),
                    actionLabel = context.getString(R.string.settings_open_settings)
                )
                if (result == SnackbarResult.ActionPerformed) context.openNotificationSettings()
            }
        }
        pendingAction = null
    }
    val withNotificationPermission = { action: () -> Unit ->
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingAction = action
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    SettingsScreen(
        settings = settings,
        onThemeModeChange = viewModel::onThemeModeChange,
        onTemperatureUnitChange = viewModel::onTemperatureUnitChange,
        onNotificationsChange = { enabled ->
            if (enabled) {
                withNotificationPermission { viewModel.onNotificationsChange(true) }
            } else {
                viewModel.onNotificationsChange(false)
            }
        },
        onSendTestNotification = { withNotificationPermission(viewModel::onSendTestNotification) },
        darkTheme = darkTheme,
        bottomPadding = bottomPadding,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

private fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
    bottomPadding: Dp = 0.dp,
    snackbarHostState: SnackbarHostState = SnackbarHostState()
) {
    SkyBackground(palette = SkyPalette.calm(darkTheme), modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp)
            )

            SettingsCard(title = stringResource(R.string.settings_theme)) {
                Choices(
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    label = { it.label },
                    onSelect = onThemeModeChange
                )
            }

            SettingsCard(title = stringResource(R.string.settings_unit)) {
                Choices(
                    options = TemperatureUnit.entries,
                    selected = settings.temperatureUnit,
                    label = { "${it.symbol} ${it.label}" },
                    onSelect = onTemperatureUnitChange
                )
            }

            SettingsCard(title = stringResource(R.string.settings_notifications)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.settings_notifications_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = onNotificationsChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.White.copy(alpha = 0.45f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.15f),
                            uncheckedBorderColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }
                TextButton(onClick = onSendTestNotification) {
                    Text(stringResource(R.string.settings_test_notification), color = Color.White)
                }
            }

            SettingsCard(title = stringResource(R.string.settings_about)) {
                Text(
                    text = stringResource(R.string.settings_about_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.height(bottomPadding + 8.dp))
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding + 8.dp)
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = LocalContentColor.current.copy(alpha = 0.75f),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

/** Birkaç seçenekten birini seçtiren cam görünümlü segmentli düğme. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Choices(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Color.White.copy(alpha = 0.30f),
                    activeContentColor = Color.White,
                    activeBorderColor = Color.White.copy(alpha = 0.6f),
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = Color.White,
                    inactiveBorderColor = Color.White.copy(alpha = 0.35f)
                ),
                label = { Text(label(option), maxLines = 1) }
            )
        }
    }
}

@Preview(heightDp = 800)
@Composable
private fun SettingsScreenPreview() {
    HavaTheme {
        SettingsScreen(
            settings = UserSettings(notificationsEnabled = true),
            onThemeModeChange = {},
            onTemperatureUnitChange = {},
            onNotificationsChange = {},
            onSendTestNotification = {}
        )
    }
}
