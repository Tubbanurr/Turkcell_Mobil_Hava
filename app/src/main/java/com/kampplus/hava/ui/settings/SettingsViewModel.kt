package com.kampplus.hava.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kampplus.hava.appContainer
import com.kampplus.hava.data.SettingsRepository
import com.kampplus.hava.data.WeatherSyncer
import com.kampplus.hava.model.TemperatureUnit
import com.kampplus.hava.model.ThemeMode
import com.kampplus.hava.model.UserSettings
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.loadState
import com.kampplus.hava.work.NotificationScheduler
import com.kampplus.hava.work.Notifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ayarlar ekranı: tercihleri DataStore'a yazar, bildirim zamanlamasını açıp kapatır. */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val scheduler: NotificationScheduler,
    private val syncer: WeatherSyncer,
    private val notifier: Notifier
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    /** "Şimdi dene" gibi işlemlerin sonucunu bildiren kısa mesaj (Snackbar). */
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun onTemperatureUnitChange(unit: TemperatureUnit) {
        viewModelScope.launch { repository.setTemperatureUnit(unit) }
    }

    fun onNotificationsChange(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
            scheduler.apply(enabled)
        }
    }

    /**
     * Beklemeden ana şehrin havasını alıp bildirimi gösterir. WorkManager'a bırakmayız:
     * orada hata sessizce yeniden denenir, kullanıcı ne olduğunu göremez.
     */
    fun onSendTestNotification() {
        viewModelScope.launch {
            _message.value = "Hava durumu alınıyor…"
            _message.value = when (val result = loadState(TAG) { syncer.sync() }) {
                is LoadState.Success -> if (notifier.canPost()) {
                    notifier.show(result.data)
                    "Bildirim gönderildi. Bildirim çubuğuna bak."
                } else {
                    MESSAGE_NOTIFICATIONS_BLOCKED
                }
                is LoadState.Error -> result.message
                else -> null
            }
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        const val MESSAGE_NOTIFICATIONS_BLOCKED = "Bildirimler kapalı. Ayarlar'dan bildirime izin ver."

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SettingsViewModel(
                    container.settingsRepository,
                    container.scheduler,
                    container.weatherSyncer,
                    container.notifier
                )
            }
        }
    }
}
