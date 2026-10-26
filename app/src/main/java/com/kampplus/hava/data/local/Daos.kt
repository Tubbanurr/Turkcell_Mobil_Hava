package com.kampplus.hava.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object): SQL'i Kotlin fonksiyonlarına çevirir. Gövdesini Room yazar.
 * `Flow` dönen sorgular tablo her değiştiğinde yeni liste yayar; ekran kendiliğinden güncellenir.
 */
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE placeKey = :placeKey)")
    fun observeExists(placeKey: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE placeKey = :placeKey")
    suspend fun delete(placeKey: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY viewedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: HistoryEntity)

    @Query(
        "DELETE FROM history WHERE placeKey NOT IN " +
            "(SELECT placeKey FROM history ORDER BY viewedAt DESC LIMIT :keep)"
    )
    suspend fun trim(keep: Int)

    @Query("DELETE FROM history")
    suspend fun clear()
}
