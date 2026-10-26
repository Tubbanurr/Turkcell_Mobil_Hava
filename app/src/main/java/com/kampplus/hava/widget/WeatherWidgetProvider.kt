package com.kampplus.hava.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.kampplus.hava.HavaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ana ekran widget'ı. `AppWidgetProvider` aslında bir **BroadcastReceiver**'dır:
 * sistem "güncelleme zamanı" yayınını gönderir, `onUpdate` çalışır.
 *
 * Receiver'ın ömrü çok kısadır; ağ isteği bitene kadar canlı kalmak için `goAsync()` kullanılır.
 */
class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        val container = (context.applicationContext as HavaApplication).container
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val result = runCatching { container.weatherSyncer.sync() }.getOrNull()
                WidgetUpdater.update(context, result)
            } finally {
                pending.finish()
            }
        }
    }
}
