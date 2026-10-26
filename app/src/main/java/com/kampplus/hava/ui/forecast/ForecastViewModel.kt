package com.kampplus.hava.ui.forecast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kampplus.hava.appContainer
import com.kampplus.hava.data.AirQualityRepository
import com.kampplus.hava.data.FavoritesRepository
import com.kampplus.hava.data.SettingsRepository
import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.model.Place
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Seçilen bir şehrin detay ekranı. Yer, navigasyon rotasından gelir.
 *
 * `SavedStateHandle` rota parametrelerini taşır; süreç öldürülüp geri dönüldüğünde de korunur.
 */
class ForecastViewModel(
    savedStateHandle: SavedStateHandle,
    weather: WeatherRepository,
    favorites: FavoritesRepository,
    airQuality: AirQualityRepository,
    private val settings: SettingsRepository
) : PlaceForecastViewModel(flowOf(savedStateHandle.toPlace()), weather, favorites, airQuality) {

    /** "Ana şehir yap": ana sekmede bu yer gösterilir, widget ve bildirim de buna göre çalışır. */
    fun onSetAsHome() {
        val current = currentPlace() ?: return
        viewModelScope.launch {
            settings.setHomePlace(current)
            showMessage("${current.name} ana şehir yapıldı.")
        }
    }

    companion object {
        /** Rota parametrelerinin adları; [com.kampplus.hava.ui.HavaNavHost] ile ortak. */
        const val ARG_NAME = "name"
        const val ARG_SUBTITLE = "subtitle"
        const val ARG_LATITUDE = "latitude"
        const val ARG_LONGITUDE = "longitude"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ForecastViewModel(
                    createSavedStateHandle(),
                    container.weatherRepository,
                    container.favoritesRepository,
                    container.airQualityRepository,
                    container.settingsRepository
                )
            }
        }
    }
}

private fun SavedStateHandle.toPlace() = Place(
    name = get<String>(ForecastViewModel.ARG_NAME).orEmpty(),
    subtitle = get<String>(ForecastViewModel.ARG_SUBTITLE).orEmpty(),
    latitude = get<String>(ForecastViewModel.ARG_LATITUDE)?.toDoubleOrNull() ?: 0.0,
    longitude = get<String>(ForecastViewModel.ARG_LONGITUDE)?.toDoubleOrNull() ?: 0.0
)
