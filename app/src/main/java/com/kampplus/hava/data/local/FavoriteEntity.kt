package com.kampplus.hava.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room'da bir tablo = bir `@Entity` sınıfı. Favori şehirler `favorites` tablosunda durur.
 * Birincil anahtar yerin koordinat anahtarıdır; aynı yer iki kez eklenemez.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val placeKey: String,
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val addedAt: Long
)

/** Son görüntülenen yerler (`history` tablosu). */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val placeKey: String,
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val viewedAt: Long
)
