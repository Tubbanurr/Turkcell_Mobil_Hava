package com.kampplus.hava.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Uygulamanın SQLite veritabanı. Tablo eklendiğinde `version` artırılır ve migration yazılır. */
@Database(entities = [FavoriteEntity::class, HistoryEntity::class], version = 1, exportSchema = false)
abstract class HavaDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun historyDao(): HistoryDao

    companion object {
        fun create(context: Context): HavaDatabase = Room.databaseBuilder(context, HavaDatabase::class.java, "hava.db").build()
    }
}
