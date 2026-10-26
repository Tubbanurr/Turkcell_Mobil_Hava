package com.kampplus.hava.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kampplus.hava.MainActivity
import com.kampplus.hava.R
import com.kampplus.hava.data.SyncResult
import com.kampplus.hava.model.WeatherCondition

/** Widget'ın görünümünü [SyncResult]'a göre çizer. Veri yoksa (`null`) "alınamadı" gösterilir. */
object WidgetUpdater {

    fun update(context: Context, result: SyncResult?) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val views = buildViews(context, result)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    private fun buildViews(context: Context, result: SyncResult?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_weather)
        if (result == null) {
            views.setTextViewText(R.id.widget_emoji, "☁️")
            views.setTextViewText(R.id.widget_city, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_condition, context.getString(R.string.widget_unavailable))
            views.setTextViewText(R.id.widget_range, "")
            views.setTextViewText(R.id.widget_temp, "--")
        } else {
            val snapshot = result.snapshot
            val unit = result.unit
            views.setTextViewText(R.id.widget_emoji, snapshot.condition.emoji(snapshot.isDay))
            views.setTextViewText(R.id.widget_city, snapshot.place.name)
            views.setTextViewText(R.id.widget_condition, snapshot.condition.label)
            views.setTextViewText(
                R.id.widget_range,
                if (snapshot.maxC != null && snapshot.minC != null) "${unit.format(snapshot.maxC)} / ${unit.format(snapshot.minC)}" else ""
            )
            views.setTextViewText(R.id.widget_temp, unit.format(snapshot.temperatureC))
            views.setInt(R.id.widget_root, "setBackgroundResource", backgroundFor(snapshot.condition, snapshot.isDay))
        }
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)
        return views
    }

    private fun backgroundFor(condition: WeatherCondition, isDay: Boolean): Int = when (condition) {
        WeatherCondition.CLEAR, WeatherCondition.MAINLY_CLEAR ->
            if (isDay) R.drawable.widget_bg_clear else R.drawable.widget_bg_night
        WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS -> R.drawable.widget_bg_rain
        WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS -> R.drawable.widget_bg_snow
        WeatherCondition.THUNDERSTORM -> R.drawable.widget_bg_storm
        else -> if (isDay) R.drawable.widget_bg_cloudy else R.drawable.widget_bg_night
    }
}
