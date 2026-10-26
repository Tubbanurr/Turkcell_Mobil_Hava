package com.kampplus.hava.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kampplus.hava.HavaApplication
import com.kampplus.hava.widget.WidgetUpdater
import java.io.IOException
import retrofit2.HttpException

/**
 * Arka planda çalışan iş: ana şehrin havasını alır, bildirim gösterir, widget'ı tazeler.
 *
 * `CoroutineWorker.doWork` suspend olduğu için ağ çağrısı doğrudan yazılır.
 * `Result.retry()` WorkManager'a "biraz sonra yeniden dene" der.
 */
class WeatherSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as HavaApplication).container
        return try {
            val result = container.weatherSyncer.sync()
            container.notifier.show(result)
            WidgetUpdater.update(applicationContext, result)
            Result.success()
        } catch (e: IOException) {
            Log.w(TAG, "Arka plan güncellemesi başarısız: bağlantı", e)
            Result.retry()
        } catch (e: HttpException) {
            Log.w(TAG, "Arka plan güncellemesi başarısız: HTTP ${e.code()}", e)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "WeatherSyncWorker"
    }
}
