package com.kampplus.hava.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kampplus.hava.data.AirQualityRepository
import com.kampplus.hava.data.FavoritesRepository
import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.model.AirQuality
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.Place
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.loadState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tahmin ekranının tek bakışta durumu: hangi yer, tahmin yükleme durumu, favori mi?
 *
 * @param airQuality Hava kalitesi/polen. Yardımcı bilgi olduğu için yüklenemezse hata göstermeyiz,
 * yalnızca kart gizlenir (`null`).
 * @param isRefreshing Aşağı çekip yenilerken true; içerik ekranda kalır, iskelet gösterilmez.
 */
data class ForecastUiState(
    val place: Place? = null,
    val forecast: LoadState<Forecast> = LoadState.Loading,
    val isFavorite: Boolean = false,
    val airQuality: AirQuality? = null,
    val isRefreshing: Boolean = false
)

/**
 * "Bir yerin tahminini göster" davranışının ortak gövdesi.
 *
 * Hangi yerin gösterileceğini alt sınıflar belirler ([places] akışı): detay ekranı rotadan,
 * ana ekran ayarlardaki ana şehirden alır. Yükleme, yeniden deneme ve favori mantığı burada
 * bir kez yazılır (Template Method).
 */
abstract class PlaceForecastViewModel(
    places: Flow<Place>,
    private val weather: WeatherRepository,
    private val favorites: FavoritesRepository,
    private val airQualityRepository: AirQualityRepository
) : ViewModel() {

    private val place = MutableStateFlow<Place?>(null)
    private val forecast = MutableStateFlow<LoadState<Forecast>>(LoadState.Loading)
    private val airQuality = MutableStateFlow<AirQuality?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private var loadJob: Job? = null
    private var airJob: Job? = null

    private val _message = MutableStateFlow<String?>(null)

    /** Bir kez gösterilip silinen kısa bilgi mesajı (Snackbar). */
    val message: StateFlow<String?> = _message.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val isFavorite: Flow<Boolean> = place.flatMapLatest { current ->
        if (current == null) flowOf(false) else favorites.isFavorite(current)
    }

    val uiState: StateFlow<ForecastUiState> = combine(place, forecast, isFavorite, airQuality, isRefreshing, ::ForecastUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, ForecastUiState())

    init {
        viewModelScope.launch {
            places.distinctUntilChanged().collect {
                place.value = it
                reload()
            }
        }
    }

    /** Hata ekranındaki "Tekrar dene" buraya bağlanır. */
    fun onRetry() = reload()

    /** Aşağı çekerek yenileme: eski içerik ekranda kalır, hata olursa yalnızca mesaj gösterilir. */
    fun onRefresh() = reload(keepContent = true)

    fun onToggleFavorite() {
        val current = place.value ?: return
        viewModelScope.launch { favorites.toggle(current) }
    }

    fun onMessageShown() {
        _message.value = null
    }

    protected fun showMessage(text: String) {
        _message.value = text
    }

    protected fun currentPlace(): Place? = place.value

    private fun reload(keepContent: Boolean = false) {
        val target = place.value ?: return
        val previous = forecast.value
        val refreshing = keepContent && previous is LoadState.Success
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (refreshing) isRefreshing.value = true else forecast.value = LoadState.Loading
            val result = loadState(TAG) { weather.getForecast(target.latitude, target.longitude) }
            if (refreshing && result is LoadState.Error) {
                showMessage(result.message)
            } else {
                forecast.value = result
            }
            isRefreshing.value = false
        }
        loadAirQuality(target)
    }

    private fun loadAirQuality(target: Place) {
        airJob?.cancel()
        airJob = viewModelScope.launch {
            val result = loadState(TAG) { airQualityRepository.getAirQuality(target.latitude, target.longitude) }
            airQuality.value = (result as? LoadState.Success)?.data
        }
    }

    private companion object {
        const val TAG = "ForecastViewModel"
    }
}
