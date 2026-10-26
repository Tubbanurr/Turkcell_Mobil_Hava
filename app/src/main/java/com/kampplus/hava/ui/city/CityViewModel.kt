package com.kampplus.hava.ui.city

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kampplus.hava.appContainer
import com.kampplus.hava.data.CityRepository
import com.kampplus.hava.data.FavoritesRepository
import com.kampplus.hava.data.HistoryRepository
import com.kampplus.hava.model.City
import com.kampplus.hava.model.Place
import com.kampplus.hava.model.toPlace
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.emptyIfNoItems
import com.kampplus.hava.ui.loadState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Şehirler sekmesinin ekran durumu.
 *
 * @param results `null` = henüz arama yapılmadı (favoriler ve geçmiş gösterilir).
 */
data class CityUiState(
    val query: String = "",
    val results: LoadState<List<City>>? = null,
    val favorites: List<Place> = emptyList(),
    val history: List<Place> = emptyList()
)

/**
 * Şehir arama + favoriler + geçmiş.
 *
 * Görev dağılımı: ekran çizer, ViewModel kararları verir, repository'ler veriyi getirir.
 */
class CityViewModel(
    private val repository: CityRepository,
    private val favoritesRepository: FavoritesRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val results = MutableStateFlow<LoadState<List<City>>?>(null)
    private var searchJob: Job? = null

    val uiState: StateFlow<CityUiState> = combine(
        query,
        results,
        favoritesRepository.favorites,
        historyRepository.recent,
        ::CityUiState
    ).stateIn(viewModelScope, SharingStarted.Eagerly, CityUiState())

    init {
        // Kullanıcı yazmayı bırakınca (DEBOUNCE) kendiliğinden arar; her harfte istek atılmaz.
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            query.debounce(SEARCH_DEBOUNCE_MS)
                .map { it.trim() }
                .distinctUntilChanged()
                .collect { text ->
                    if (text.length >= MIN_QUERY_LENGTH) runSearch(text) else clearResults()
                }
        }
    }

    /** Kullanıcı arama kutusuna her harf yazdığında çağrılır. */
    fun onQueryChange(newQuery: String) {
        query.value = newQuery
        if (newQuery.isBlank()) clearResults()
    }

    /** Klavyedeki "Ara" tuşu ve "Tekrar dene": beklemeden hemen arar. */
    fun search() {
        val text = query.value.trim()
        if (text.length >= MIN_QUERY_LENGTH) runSearch(text)
    }

    /** Bir yer açıldığında geçmişe yazılır. */
    fun onPlaceOpened(place: Place) {
        viewModelScope.launch { historyRepository.record(place) }
    }

    fun onCityOpened(city: City) = onPlaceOpened(city.toPlace())

    fun onRemoveFavorite(place: Place) {
        viewModelScope.launch { favoritesRepository.remove(place) }
    }

    fun onClearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    private fun runSearch(text: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            results.value = LoadState.Loading
            results.value = loadState(TAG) { repository.search(text) }.emptyIfNoItems()
        }
    }

    private fun clearResults() {
        searchJob?.cancel()
        results.value = null
    }

    companion object {
        private const val TAG = "CityViewModel"
        const val SEARCH_DEBOUNCE_MS = 400L
        const val MIN_QUERY_LENGTH = 2

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                CityViewModel(container.cityRepository, container.favoritesRepository, container.historyRepository)
            }
        }
    }
}
