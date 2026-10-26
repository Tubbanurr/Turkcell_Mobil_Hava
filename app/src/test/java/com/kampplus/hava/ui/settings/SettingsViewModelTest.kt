package com.kampplus.hava.ui.settings

import com.kampplus.hava.data.SyncResult
import com.kampplus.hava.data.WeatherSyncer
import com.kampplus.hava.model.TemperatureUnit
import com.kampplus.hava.model.ThemeMode
import com.kampplus.hava.testing.FakeSettingsRepository
import com.kampplus.hava.testing.FakeWeatherRepository
import com.kampplus.hava.testing.MainDispatcherRule
import com.kampplus.hava.work.NotificationScheduler
import com.kampplus.hava.work.Notifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeScheduler : NotificationScheduler {
        var enabled: Boolean? = null

        override fun apply(enabled: Boolean) {
            this.enabled = enabled
        }
    }

    private class FakeNotifier(var allowed: Boolean = true) : Notifier {
        var shown: SyncResult? = null

        override fun ensureChannel() = Unit

        override fun canPost() = allowed

        override fun show(result: SyncResult) {
            shown = result
        }
    }

    private val scheduler = FakeScheduler()
    private val notifier = FakeNotifier()
    private val weather = FakeWeatherRepository()
    private val settings = FakeSettingsRepository()
    private fun createViewModel() = SettingsViewModel(settings, scheduler, WeatherSyncer(settings, weather), notifier)

    @Test
    fun `tema ve birim kaydedilir`() = runTest {
        val viewModel = createViewModel()
        viewModel.onThemeModeChange(ThemeMode.DARK)
        viewModel.onTemperatureUnitChange(TemperatureUnit.FAHRENHEIT)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals(TemperatureUnit.FAHRENHEIT, viewModel.uiState.value.temperatureUnit)
    }

    @Test
    fun `bildirim acilinca zamanlayici calisir`() = runTest {
        val viewModel = createViewModel()
        viewModel.onNotificationsChange(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.notificationsEnabled)
        assertEquals(true, scheduler.enabled)
    }

    @Test
    fun `test bildirimi hemen gosterilir ve kullaniciya bildirilir`() = runTest {
        val viewModel = createViewModel()
        viewModel.onSendTestNotification()
        advanceUntilIdle()

        assertTrue(notifier.shown != null)
        assertEquals("Bildirim gönderildi. Bildirim çubuğuna bak.", viewModel.message.value)
    }

    @Test
    fun `bildirim kapaliysa gonderilmez ve neden soylenir`() = runTest {
        notifier.allowed = false
        val viewModel = createViewModel()
        viewModel.onSendTestNotification()
        advanceUntilIdle()

        assertEquals(null, notifier.shown)
        assertEquals(SettingsViewModel.MESSAGE_NOTIFICATIONS_BLOCKED, viewModel.message.value)
    }

    @Test
    fun `ag hatasi sessiz kalmaz`() = runTest {
        weather.error = java.io.IOException("no network")
        val viewModel = createViewModel()
        viewModel.onSendTestNotification()
        advanceUntilIdle()

        assertEquals(null, notifier.shown)
        assertEquals("İnternet bağlantını kontrol et.", viewModel.message.value)
    }
}
