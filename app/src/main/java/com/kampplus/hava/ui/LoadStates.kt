package com.kampplus.hava.ui

import android.util.Log
import com.google.gson.JsonSyntaxException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/**
 * Bir ağ işini çalıştırır ve sonucu/hatayı [LoadState]'e çevirir.
 *
 * Kullanıcıya sade mesaj gösterilir, teknik ayrıntı Logcat'e yazılır.
 * İptal (CancellationException) hata sayılmaz: yeniden fırlatılır ki coroutine düzgün dursun.
 */
suspend fun <T> loadState(tag: String, block: suspend () -> T): LoadState<T> = try {
    LoadState.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    // İnternet yok, DNS çözülemedi veya zaman aşımı.
    Log.e(tag, "Bağlantı hatası", e)
    LoadState.Error(MESSAGE_NO_CONNECTION)
} catch (e: HttpException) {
    // Sunucu yanıt verdi ama 4xx/5xx döndü.
    Log.e(tag, "HTTP ${e.code()}", e)
    LoadState.Error(MESSAGE_SERVER)
} catch (e: JsonSyntaxException) {
    // Beklenmeyen JSON: model ile yanıt uyuşmuyor.
    Log.e(tag, "Yanıt çözümlenemedi", e)
    LoadState.Error(MESSAGE_GENERIC)
} catch (e: IllegalStateException) {
    // ForecastResponse.toForecast(), "current" bloğu yoksa bunu fırlatır.
    Log.e(tag, "Eksik veri", e)
    LoadState.Error(MESSAGE_GENERIC)
}

/** Cevap geldi ama liste boşsa bu hata değil, "Empty" durumudur. */
fun <T> LoadState<List<T>>.emptyIfNoItems(): LoadState<List<T>> = if (this is LoadState.Success && data.isEmpty()) LoadState.Empty else this

const val MESSAGE_NO_CONNECTION = "İnternet bağlantını kontrol et."
const val MESSAGE_SERVER = "Sunucu şu an cevap vermiyor."
const val MESSAGE_GENERIC = "Bir şeyler ters gitti. Tekrar dene."
