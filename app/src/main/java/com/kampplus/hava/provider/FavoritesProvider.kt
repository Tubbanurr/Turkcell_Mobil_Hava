package com.kampplus.hava.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.kampplus.hava.HavaApplication
import kotlinx.coroutines.runBlocking

/**
 * Favori şehirleri salt-okunur olarak dışarı açan ContentProvider.
 *
 * ContentProvider, bir uygulamanın verisini başka uygulamalara **tablo gibi** sunmanın
 * standart yoludur. Burada `exported="false"` olduğundan yalnızca bu uygulama erişebilir;
 * dışarı açmak istersek manifestte izin tanımlamak gerekir.
 *
 * Adres: `content://com.kampplus.hava.provider/favorites`
 */
class FavoritesProvider : ContentProvider() {

    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, PATH_FAVORITES, CODE_FAVORITES)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        if (matcher.match(uri) != CODE_FAVORITES) return null
        val container = (context?.applicationContext as? HavaApplication)?.container ?: return null
        val cursor = MatrixCursor(COLUMNS)
        // query() zaten arka plandaki bir binder thread'inde çalışır; burada beklemek güvenlidir.
        runBlocking { container.favoriteDao.getAll() }.forEach {
            cursor.addRow(arrayOf(it.placeKey, it.name, it.subtitle, it.latitude, it.longitude))
        }
        return cursor
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_FAVORITES"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private companion object {
        const val AUTHORITY = "com.kampplus.hava.provider"
        const val PATH_FAVORITES = "favorites"
        const val CODE_FAVORITES = 1
        val COLUMNS = arrayOf("place_key", "name", "subtitle", "latitude", "longitude")
    }
}
