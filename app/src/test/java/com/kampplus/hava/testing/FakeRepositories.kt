package com.kampplus.hava.testing

import com.kampplus.hava.data.AirQualityRepository
import com.kampplus.hava.data.CityRepository
import com.kampplus.hava.data.FavoritesRepository
import com.kampplus.hava.data.HistoryRepository
import com.kampplus.hava.data.SettingsRepository
import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.model.AirQuality
import com.kampplus.hava.model.City
import com.kampplus.hava.model.CurrentWeather
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.Place
import com.kampplus.hava.model.TemperatureUnit
import com.kampplus.hava.model.ThemeMode
import com.kampplus.hava.model.UserSettings
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Testlerde gerçek repository yerine kullanılan sahteler.
 *
 * Repository'yi arayüz olarak tanımlamamızın sebebi tam olarak budur: ViewModel
 * testleri internete çıkmadan, istenen her sonucu (başarı / boş / hata)
 * kolayca üretebilir.
 */

/**
 * @param result Her çağrıda dönecek liste.
 * @param error Doluysa [search] bu hatayı fırlatır.
 */
class FakeCityRepository(
    var result: List<City> = emptyList(),
    var error: Throwable? = null
) : CityRepository {

    /** Son aranan metin; ViewModel'in doğru sorguyu ilettiğini doğrulamak için. */
    var lastQuery: String? = null
        private set

    /** Kaç kez arama yapıldığı; "Tekrar dene" davranışını doğrulamak için. */
    var callCount: Int = 0
        private set

    override suspend fun search(query: String): List<City> {
        lastQuery = query
        callCount++
        error?.let { throw it }
        return result
    }
}

/** @param error Doluysa [getForecast] bu hatayı fırlatır. */
class FakeWeatherRepository(
    var result: Forecast = sampleForecast(),
    var error: Throwable? = null
) : WeatherRepository {

    var callCount: Int = 0
        private set

    override suspend fun getForecast(latitude: Double, longitude: Double): Forecast {
        callCount++
        error?.let { throw it }
        return result
    }
}

/** @param error Doluysa [getAirQuality] bu hatayı fırlatır. */
class FakeAirQualityRepository(
    var result: AirQuality? = AirQuality(europeanAqi = 35, pm25 = 8.0, pm10 = 14.0),
    var error: Throwable? = null
) : AirQualityRepository {
    override suspend fun getAirQuality(latitude: Double, longitude: Double): AirQuality? {
        error?.let { throw it }
        return result
    }
}

/** Testlerde tekrar tekrar yazmamak için hazır bir şehir. */
fun sampleCity(id: Long = 323786L, name: String = "Ankara") = City(
    id = id,
    name = name,
    latitude = 39.91987,
    longitude = 32.85427,
    countryCode = "TR",
    country = "Türkiye",
    admin1 = "Ankara"
)

/** Testlerde tekrar tekrar yazmamak için hazır bir tahmin. */
fun sampleForecast() = Forecast(
    current = CurrentWeather(
        temperatureC = 21.0,
        weatherCode = 0,
        observedAt = LocalDateTime.parse("2026-09-24T11:30")
    ),
    hourly = emptyList(),
    daily = emptyList()
)

/** Bellekte tutulan sahte favori listesi. */
class FakeFavoritesRepository : FavoritesRepository {
    private val items = MutableStateFlow<List<Place>>(emptyList())

    override val favorites: Flow<List<Place>> = items

    override fun isFavorite(place: Place): Flow<Boolean> = items.map { list -> list.any { it.key == place.key } }

    override suspend fun toggle(place: Place) {
        items.value = if (items.value.any { it.key == place.key }) {
            items.value.filterNot { it.key == place.key }
        } else {
            items.value + place
        }
    }

    override suspend fun remove(place: Place) {
        items.value = items.value.filterNot { it.key == place.key }
    }
}

/** Bellekte tutulan sahte arama geçmişi (en yeni başta). */
class FakeHistoryRepository : HistoryRepository {
    private val items = MutableStateFlow<List<Place>>(emptyList())

    override val recent: Flow<List<Place>> = items

    override suspend fun record(place: Place) {
        items.value = listOf(place) + items.value.filterNot { it.key == place.key }
    }

    override suspend fun clear() {
        items.value = emptyList()
    }
}

fun samplePlace(name: String = "Ankara") = Place(name, "$name, Türkiye", 39.92, 32.85)

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<UserSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        state.value = state.value.copy(temperatureUnit = unit)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        state.value = state.value.copy(notificationsEnabled = enabled)
    }

    override suspend fun setHomePlace(place: Place) {
        state.value = state.value.copy(homePlace = place)
    }
}
