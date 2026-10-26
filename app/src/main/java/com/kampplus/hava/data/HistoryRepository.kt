package com.kampplus.hava.data

import com.kampplus.hava.data.local.HistoryDao
import com.kampplus.hava.data.local.HistoryEntity
import com.kampplus.hava.model.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Son görüntülenen yerler ("Son aramalar"). */
interface HistoryRepository {
    val recent: Flow<List<Place>>

    suspend fun record(place: Place)

    suspend fun clear()
}

class RoomHistoryRepository(
    private val dao: HistoryDao,
    private val clock: () -> Long = System::currentTimeMillis
) : HistoryRepository {

    override val recent: Flow<List<Place>> = dao.observeRecent(MAX_ITEMS).map { list ->
        list.map { Place(it.name, it.subtitle, it.latitude, it.longitude) }
    }

    override suspend fun record(place: Place) {
        dao.upsert(HistoryEntity(place.key, place.name, place.subtitle, place.latitude, place.longitude, clock()))
        dao.trim(MAX_ITEMS)
    }

    override suspend fun clear() = dao.clear()

    private companion object {
        const val MAX_ITEMS = 10
    }
}
