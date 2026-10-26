package com.kampplus.hava.ui.home

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kampplus.hava.appContainer
import com.kampplus.hava.data.AirQualityRepository
import com.kampplus.hava.data.FavoritesRepository
import com.kampplus.hava.data.LocationProvider
import com.kampplus.hava.data.SettingsRepository
import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.ui.forecast.PlaceForecastViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Ana sekme: ayarlardaki ana şehrin havası. Ana şehir değişince (ör. konum butonu)
 * ekran kendiliğinden yeni yeri yükler.
 */
class HomeViewModel(
    private val settings: SettingsRepository,
    weather: WeatherRepository,
    favorites: FavoritesRepository,
    airQuality: AirQualityRepository,
    private val locationProvider: LocationProvider
) : PlaceForecastViewModel(settings.settings.map { it.homePlace }, weather, favorites, airQuality) {

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    /** Konum izni verildikten sonra çağrılır: GPS konumunu ana şehir yapar. */
    fun onLocateMe() {
        if (_isLocating.value) return
        viewModelScope.launch {
            _isLocating.value = true
            val place = locationProvider.currentPlace()
            if (place != null) {
                settings.setHomePlace(place)
            } else {
                showMessage("Konum alınamadı. Konum servisinin açık olduğundan emin ol.")
            }
            _isLocating.value = false
        }
    }

    fun onLocationPermissionDenied() = showMessage("Konum izni verilmedi.")

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HomeViewModel(
                    container.settingsRepository,
                    container.weatherRepository,
                    container.favoritesRepository,
                    container.airQualityRepository,
                    container.locationProvider
                )
            }
        }
    }
}
