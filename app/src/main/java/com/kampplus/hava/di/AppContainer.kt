package com.kampplus.hava.di

import android.content.Context
import com.kampplus.hava.data.AirQualityRepository
import com.kampplus.hava.data.CityRepository
import com.kampplus.hava.data.DataStoreSettingsRepository
import com.kampplus.hava.data.FavoritesRepository
import com.kampplus.hava.data.FusedLocationProvider
import com.kampplus.hava.data.HistoryRepository
import com.kampplus.hava.data.LocationProvider
import com.kampplus.hava.data.OpenMeteoAirQualityRepository
import com.kampplus.hava.data.OpenMeteoCityRepository
import com.kampplus.hava.data.OpenMeteoWeatherRepository
import com.kampplus.hava.data.RoomFavoritesRepository
import com.kampplus.hava.data.RoomHistoryRepository
import com.kampplus.hava.data.SettingsRepository
import com.kampplus.hava.data.WeatherRepository
import com.kampplus.hava.data.WeatherSyncer
import com.kampplus.hava.data.local.HavaDatabase
import com.kampplus.hava.data.settingsDataStore
import com.kampplus.hava.work.NotificationScheduler
import com.kampplus.hava.work.Notifier
import com.kampplus.hava.work.WeatherNotifier
import com.kampplus.hava.work.WorkManagerNotificationScheduler

/**
 * Elle kurulan bağımlılık kabı (manual DI).
 *
 * Her sınıf ihtiyacı olan nesneleri **kurucudan** ister (Dependency Inversion); kimin kimi
 * üreteceği tek yerde, burada yazılıdır. Hilt/Koin gibi kütüphaneler bu işi otomatikleştirir;
 * küçük bir uygulamada elle yazmak hem yeterli hem öğreticidir.
 *
 * `lazy`: nesne ilk kullanıldığında kurulur, açılışı yavaşlatmaz.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: HavaDatabase by lazy { HavaDatabase.create(appContext) }

    val cityRepository: CityRepository by lazy { OpenMeteoCityRepository() }
    val weatherRepository: WeatherRepository by lazy { OpenMeteoWeatherRepository() }
    val airQualityRepository: AirQualityRepository by lazy { OpenMeteoAirQualityRepository() }
    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(appContext.settingsDataStore) }
    val favoritesRepository: FavoritesRepository by lazy { RoomFavoritesRepository(database.favoriteDao()) }
    val historyRepository: HistoryRepository by lazy { RoomHistoryRepository(database.historyDao()) }
    val locationProvider: LocationProvider by lazy { FusedLocationProvider(appContext) }

    val weatherSyncer: WeatherSyncer by lazy { WeatherSyncer(settingsRepository, weatherRepository) }
    val notifier: Notifier by lazy { WeatherNotifier(appContext) }
    val scheduler: NotificationScheduler by lazy { WorkManagerNotificationScheduler(appContext) }

    /** ContentProvider yalnızca okuma yapar; DAO'ya doğrudan erişim burada açılır. */
    val favoriteDao get() = database.favoriteDao()
}
