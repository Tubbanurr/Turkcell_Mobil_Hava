package com.kampplus.hava.ui.forecast

import androidx.lifecycle.SavedStateHandle
import com.kampplus.hava.testing.FakeAirQualityRepository
import com.kampplus.hava.testing.FakeFavoritesRepository
import com.kampplus.hava.testing.FakeSettingsRepository
import com.kampplus.hava.testing.FakeWeatherRepository
import com.kampplus.hava.testing.MainDispatcherRule
import com.kampplus.hava.ui.LoadState
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tahmin ekranının durumlarını ve rota parametrelerini okuyuşunu doğrular.
 *
 * `SavedStateHandle` doğrudan oluşturulabildiği için navigasyonu ayağa kaldırmadan
 * test edebiliyoruz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForecastViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeWeatherRepository()
    private val favorites = FakeFavoritesRepository()
    private val airQuality = FakeAirQualityRepository()

    private fun createViewModel(
        name: String = "Ankara",
        subtitle: String = "Ankara, Türkiye",
        latitude: String = "39.91987",
        longitude: String = "32.85427"
    ) = ForecastViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                ForecastViewModel.ARG_NAME to name,
                ForecastViewModel.ARG_SUBTITLE to subtitle,
                ForecastViewModel.ARG_LATITUDE to latitude,
                ForecastViewModel.ARG_LONGITUDE to longitude
            )
        ),
        weather = repository,
        favorites = favorites,
        airQuality = airQuality,
        settings = FakeSettingsRepository()
    )

    @Test
    fun `rota parametreleri okunur`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val place = viewModel.uiState.value.place
        assertEquals("Ankara", place?.name)
        assertEquals("Ankara, Türkiye", place?.subtitle)
    }

    @Test
    fun `baslangicta Loading gosterilir`() = runTest {
        val viewModel = createViewModel()

        assertEquals(LoadState.Loading, viewModel.uiState.value.forecast)
    }

    @Test
    fun `tahmin gelince Success durumuna gecilir`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value.forecast
        assertTrue("Success bekleniyordu, gelen: $state", state is LoadState.Success)
        assertEquals(21.0, (state as LoadState.Success).data.current.temperatureC, 0.01)
    }

    @Test
    fun `baglanti hatasi Error durumuna cevrilir`() = runTest {
        repository.error = IOException("no network")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            "İnternet bağlantını kontrol et.",
            (viewModel.uiState.value.forecast as LoadState.Error).message
        )
    }

    @Test
    fun `eksik current blogu Error durumuna cevrilir`() = runTest {
        // ForecastResponse.toForecast() bozuk yanıtta IllegalStateException fırlatır.
        repository.error = IllegalStateException("Forecast response has no current block")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.forecast is LoadState.Error)
    }

    @Test
    fun `tekrar dene yeni istek baslatir`() = runTest {
        repository.error = IOException("no network")
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.forecast is LoadState.Error)

        repository.error = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.forecast is LoadState.Success)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `gecersiz koordinat uygulamayi cokertmez`() = runTest {
        val viewModel = createViewModel(latitude = "abc", longitude = "")
        advanceUntilIdle()

        // toDoubleOrNull() null dönünce 0.0 kullanılır; istek yine de yapılır.
        assertTrue(viewModel.uiState.value.forecast is LoadState.Success)
    }

    @Test
    fun `favori degistirilir`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.isFavorite)

        viewModel.onToggleFavorite()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isFavorite)
    }

    @Test
    fun `hava kalitesi yuklenir`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(35, viewModel.uiState.value.airQuality?.europeanAqi)
    }

    @Test
    fun `hava kalitesi hatasi tahmini bozmaz`() = runTest {
        airQuality.error = IOException("no network")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.forecast is LoadState.Success)
        assertEquals(null, viewModel.uiState.value.airQuality)
    }

    @Test
    fun `yenileme basarisiz olursa eski tahmin ekranda kalir`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.error = IOException("no network")
        viewModel.onRefresh()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.forecast is LoadState.Success)
        assertTrue(!viewModel.uiState.value.isRefreshing)
        assertEquals("İnternet bağlantını kontrol et.", viewModel.message.value)
    }
}
