package com.kampplus.hava.data

import com.kampplus.hava.data.local.FavoriteDao
import com.kampplus.hava.data.local.FavoriteEntity
import com.kampplus.hava.model.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Favori yerlerin kalıcı listesi. Ekranlar Room'u değil yalnızca bu arayüzü tanır. */
interface FavoritesRepository {
    val favorites: Flow<List<Place>>

    fun isFavorite(place: Place): Flow<Boolean>

    /** Favorideyse çıkarır, değilse ekler. */
    suspend fun toggle(place: Place)

    suspend fun remove(place: Place)
}

class RoomFavoritesRepository(
    private val dao: FavoriteDao,
    private val clock: () -> Long = System::currentTimeMillis
) : FavoritesRepository {

    override val favorites: Flow<List<Place>> = dao.observeAll().map { list -> list.map { it.toPlace() } }

    override fun isFavorite(place: Place): Flow<Boolean> = dao.observeExists(place.key)

    override suspend fun toggle(place: Place) {
        if (dao.observeExists(place.key).first()) remove(place) else dao.upsert(place.toFavoriteEntity(clock()))
    }

    override suspend fun remove(place: Place) = dao.delete(place.key)
}

private fun FavoriteEntity.toPlace() = Place(name, subtitle, latitude, longitude)

private fun Place.toFavoriteEntity(now: Long) = FavoriteEntity(key, name, subtitle, latitude, longitude, now)
