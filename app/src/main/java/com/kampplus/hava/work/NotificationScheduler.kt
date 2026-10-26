package com.kampplus.hava.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Günlük hava bildirimini zamanlayan taraf. Arayüz olduğu için ViewModel testinde sahtesi kullanılır. */
interface NotificationScheduler {
    /** Günlük bildirimi açar/kapatır. */
    fun apply(enabled: Boolean)
}

/**
 * WorkManager: uygulama kapalıyken bile, sistem uygun görünce çalışan garantili arka plan işi.
 * Telefon yeniden başlasa da iş kaybolmaz.
 */
class WorkManagerNotificationScheduler(context: Context) : NotificationScheduler {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun apply(enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(DAILY_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilNext(NOTIFICATION_HOUR, LocalDateTime.now()))
            .setConstraints(networkConstraint)
            .build()
        workManager.enqueueUniquePeriodicWork(DAILY_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private val networkConstraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private companion object {
        const val DAILY_WORK_NAME = "daily_weather_notification"
        const val NOTIFICATION_HOUR = 8
    }
}

/** Bir sonraki saat başı (örn. 08:00) ne kadar sonra? Bugünkü saat geçtiyse yarınkine bakar. */
fun delayUntilNext(hour: Int, now: LocalDateTime): Duration {
    var target = now.toLocalDate().atTime(hour, 0)
    if (!target.isAfter(now)) target = target.plusDays(1)
    return Duration.between(now, target)
}
