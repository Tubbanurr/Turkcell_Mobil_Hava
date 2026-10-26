package com.kampplus.hava.ui.city

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kampplus.hava.R
import com.kampplus.hava.model.City
import com.kampplus.hava.model.Place
import com.kampplus.hava.model.toPlace
import com.kampplus.hava.ui.LoadState
import com.kampplus.hava.ui.component.EmptyView
import com.kampplus.hava.ui.component.ErrorView
import com.kampplus.hava.ui.component.ShimmerList
import com.kampplus.hava.ui.theme.HavaTheme
import com.kampplus.hava.ui.weather.SkyBackground
import com.kampplus.hava.ui.weather.SkyPalette

/**
 * Şehirler sekmesinin "bağlayıcı" katmanı.
 *
 * Tek işi ViewModel'i edinip state'i toplamak ve [CityScreen]'e aktarmaktır. Bu ayrım
 * sayesinde [CityScreen] ViewModel tanımaz: @Preview'da ve testte elle state verilerek çalışır.
 *
 * `collectAsStateWithLifecycle`: StateFlow'u Compose state'ine çevirir ve uygulama
 * arka plandayken dinlemeyi durdurur.
 */
@Composable
fun CityRoute(
    onPlaceClick: (Place) -> Unit,
    darkTheme: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: CityViewModel = viewModel(factory = CityViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val open = { place: Place ->
        viewModel.onPlaceOpened(place)
        onPlaceClick(place)
    }

    CityScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::search,
        onCityClick = { open(it.toPlace()) },
        onPlaceClick = open,
        onRemoveFavorite = viewModel::onRemoveFavorite,
        onClearHistory = viewModel::onClearHistory,
        darkTheme = darkTheme,
        bottomPadding = bottomPadding,
        modifier = modifier
    )
}

/**
 * Şehirler ekranı. Arama yapılmıyorsa favoriler ve geçmiş, yapılıyorsa sonuçlar gösterilir.
 *
 * State tutmaz, yalnızca aldığı değerleri çizer ve olayları yukarı bildirir.
 */
@Composable
fun CityScreen(
    uiState: CityUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onCityClick: (City) -> Unit,
    onPlaceClick: (Place) -> Unit,
    onRemoveFavorite: (Place) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
    bottomPadding: Dp = 0.dp
) {
    SkyBackground(palette = SkyPalette.calm(darkTheme), modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                text = stringResource(R.string.cities_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp)
            )
            SearchField(
                query = uiState.query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            Box(modifier = Modifier.fillMaxSize()) {
                val results = uiState.results
                if (results == null) {
                    SavedPlaces(uiState, onPlaceClick, onRemoveFavorite, onClearHistory, bottomPadding)
                } else {
                    // Dört durumun tamamı burada karşılanır. `sealed` olduğu için
                    // bir dalı unutursak Kotlin derleme hatası verir.
                    when (results) {
                        LoadState.Loading -> ShimmerList()
                        is LoadState.Success -> CityList(results.data, onCityClick, bottomPadding)
                        LoadState.Empty -> EmptyView(
                            title = stringResource(R.string.cities_empty_title),
                            message = stringResource(R.string.cities_empty_message),
                            modifier = Modifier.align(Alignment.Center)
                        )
                        is LoadState.Error -> ErrorView(
                            message = results.message,
                            onRetry = onSearch,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cam görünümlü arama kutusu.
 *
 * State tutmaz: değeri yukarıdan alır, her değişikliği yukarıya bildirir
 * ("değer aşağı, olay yukarı" — state hoisting).
 */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val glass = Color.White.copy(alpha = 0.18f)
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            // Temizle düğmesi yalnızca kutu doluyken görünür.
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = glass,
            unfocusedContainerColor = glass,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
            focusedLeadingIconColor = Color.White,
            unfocusedLeadingIconColor = Color.White,
            focusedTrailingIconColor = Color.White,
            unfocusedTrailingIconColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        // Klavyedeki Enter tuşu "Ara" olur.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboardController?.hide()
                onSearch()
            }
        )
    )
}

/**
 * Kaydırılabilir şehir listesi.
 *
 * `LazyColumn` tembeldir: yüzlerce şehir olsa da yalnızca ekranda görünen satırlar
 * çizilir. `key = { it.id }`: liste değişince Compose hangi satırın hangisi olduğunu bilir.
 */
@Composable
private fun CityList(cities: List<City>, onCityClick: (City) -> Unit, bottomPadding: Dp, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = listPadding(bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = cities, key = { it.id }) { city ->
            CityCard(city = city, onClick = { onCityClick(city) })
        }
    }
}

@Composable
private fun SavedPlaces(
    uiState: CityUiState,
    onPlaceClick: (Place) -> Unit,
    onRemoveFavorite: (Place) -> Unit,
    onClearHistory: () -> Unit,
    bottomPadding: Dp
) {
    if (uiState.favorites.isEmpty() && uiState.history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyView(
                title = stringResource(R.string.cities_hint_title),
                message = stringResource(R.string.cities_hint_message)
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listPadding(bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.favorites.isNotEmpty()) {
            item(key = "favorites-title") { SectionHeader(stringResource(R.string.cities_favorites)) }
            items(items = uiState.favorites, key = { "fav-${it.key}" }) { place ->
                PlaceCard(
                    place = place,
                    onClick = { onPlaceClick(place) },
                    trailing = {
                        IconButton(onClick = { onRemoveFavorite(place) }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_favorite_remove))
                        }
                    }
                )
            }
        }
        if (uiState.history.isNotEmpty()) {
            item(key = "history-title") {
                SectionHeader(stringResource(R.string.cities_recent)) {
                    TextButton(onClick = onClearHistory) {
                        Text(stringResource(R.string.action_clear), color = Color.White)
                    }
                }
            }
            items(items = uiState.history, key = { "his-${it.key}" }) { place ->
                PlaceCard(place = place, onClick = { onPlaceClick(place) })
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
        action?.invoke()
    }
}

private fun listPadding(bottom: Dp) = androidx.compose.foundation.layout.PaddingValues(
    start = 16.dp,
    end = 16.dp,
    top = 4.dp,
    bottom = bottom + 16.dp
)

@Preview
@Composable
private fun CityScreenPreview() {
    HavaTheme {
        CityScreen(
            uiState = CityUiState(
                query = "Ankara",
                results = LoadState.Success(
                    listOf(
                        City(323786, "Ankara", 39.91987, 32.85427, "TR", "Türkiye", "Ankara"),
                        City(738648, "Ankara", 41.0050, 39.7269, "TR", "Türkiye", "Trabzon")
                    )
                )
            ),
            onQueryChange = {},
            onSearch = {},
            onCityClick = {},
            onPlaceClick = {},
            onRemoveFavorite = {},
            onClearHistory = {}
        )
    }
}

@Preview
@Composable
private fun CityScreenSavedPreview() {
    HavaTheme {
        CityScreen(
            uiState = CityUiState(
                favorites = listOf(Place("İstanbul", "İstanbul, Türkiye", 41.0, 29.0)),
                history = listOf(Place("Ankara", "Ankara, Türkiye", 39.9, 32.8))
            ),
            onQueryChange = {},
            onSearch = {},
            onCityClick = {},
            onPlaceClick = {},
            onRemoveFavorite = {},
            onClearHistory = {}
        )
    }
}
