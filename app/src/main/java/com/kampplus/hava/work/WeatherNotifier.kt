package com.kampplus.hava.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kampplus.hava.MainActivity
import com.kampplus.hava.R
import com.kampplus.hava.data.SyncResult

/** Bildirim gösteren taraf. Arayüz olduğu için ViewModel testinde sahtesi kullanılır. */
interface Notifier {
    fun ensureChannel()

    /** İzin ve kanal ayarları bildirime izin veriyor mu? */
    fun canPost(): Boolean

    fun show(result: SyncResult)
}

/** Günlük hava özeti bildirimini kurar ve gösterir. */
class WeatherNotifier(private val context: Context) : Notifier {

    /** Android 8+ için bildirimler bir "kanala" aittir; kullanıcı kanalı sistem ayarlarından kısabilir. */
    override fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Kanalın önemi oluşturulduktan sonra koddan yükseltilemez; bu yüzden eskisini silip yenisini açarız.
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    /** Android 13+ bildirim iznini çalışma anında ister; izin ya da kanal kapalıysa false döner. */
    override fun canPost(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun show(result: SyncResult) {
        if (!canPost()) return
        ensureChannel()
        val snapshot = result.snapshot
        val unit = result.unit
        val range = if (snapshot.maxC != null && snapshot.minC != null) {
            " · ${unit.format(snapshot.maxC)} / ${unit.format(snapshot.minC)}"
        } else {
            ""
        }
        val rain = snapshot.precipitationProbability?.takeIf { it >= RAIN_WARNING_PERCENT }
            ?.let { " · Yağış %$it" }
            .orEmpty()
        val summary = "${snapshot.condition.emoji(snapshot.isDay)} ${snapshot.condition.label}$range$rain"
        val tips = result.advice.take(MAX_TIPS).joinToString(separator = "") { "\n${it.emoji} ${it.title}" }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_weather)
            .setContentTitle("${snapshot.place.name} · ${unit.format(snapshot.temperatureC)}")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary + tips))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val LEGACY_CHANNEL_ID = "daily_weather"
        const val CHANNEL_ID = "daily_weather_v2"
        const val MAX_TIPS = 3
        const val NOTIFICATION_ID = 1001
        const val RAIN_WARNING_PERCENT = 40
    }
}
