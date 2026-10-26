package com.kampplus.hava.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kampplus.hava.model.Place
import com.kampplus.hava.model.TemperatureUnit
import com.kampplus.hava.model.ThemeMode
import com.kampplus.hava.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Kullanıcı tercihlerinin tek kapısı. */
interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setTemperatureUnit(unit: TemperatureUnit)

    suspend fun setNotificationsEnabled(enabled: Boolean)

    suspend fun setHomePlace(place: Place)
}

/** `Context.settingsDataStore`: uygulama başına tek DataStore örneği (dosya: settings.preferences_pb). */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore, SharedPreferences'ın coroutine/Flow ile çalışan modern halidir:
 * yazma `suspend`, okuma `Flow` olduğu için ana thread'i bloklamaz.
 */
class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        val default = UserSettings()
        UserSettings(
            themeMode = prefs[THEME].toEnumOr(default.themeMode),
            temperatureUnit = prefs[UNIT].toEnumOr(default.temperatureUnit),
            notificationsEnabled = prefs[NOTIFICATIONS] ?: default.notificationsEnabled,
            homePlace = prefs.readHomePlace() ?: default.homePlace
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME] = mode.name }
    }

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        dataStore.edit { it[UNIT] = unit.name }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS] = enabled }
    }

    override suspend fun setHomePlace(place: Place) {
        dataStore.edit {
            it[HOME_NAME] = place.name
            it[HOME_SUBTITLE] = place.subtitle
            it[HOME_LAT] = place.latitude
            it[HOME_LON] = place.longitude
            it[HOME_IS_CURRENT] = place.isCurrentLocation
        }
    }

    private fun Preferences.readHomePlace(): Place? {
        val name = this[HOME_NAME] ?: return null
        val lat = this[HOME_LAT] ?: return null
        val lon = this[HOME_LON] ?: return null
        return Place(name, this[HOME_SUBTITLE].orEmpty(), lat, lon, this[HOME_IS_CURRENT] ?: false)
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val UNIT = stringPreferencesKey("temperature_unit")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val HOME_NAME = stringPreferencesKey("home_name")
        val HOME_SUBTITLE = stringPreferencesKey("home_subtitle")
        val HOME_LAT = doublePreferencesKey("home_lat")
        val HOME_LON = doublePreferencesKey("home_lon")
        val HOME_IS_CURRENT = booleanPreferencesKey("home_is_current")
    }
}

/** Kayıtlı metin bozuksa ya da yoksa uygulama çökmesin, varsayılana dönsün. */
private inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
