package com.kampplus.hava.ui.forecast

import android.content.Context
import android.content.Intent
import com.kampplus.hava.model.Forecast
import com.kampplus.hava.model.Place
import com.kampplus.hava.model.TemperatureUnit
import com.kampplus.hava.model.WeatherCondition

/**
 * Paylaşılacak kısa hava özeti. Saf bir fonksiyondur (Android'e bağımlı değil), bu yüzden
 * birim testle doğrulanır.
 */
fun buildShareText(place: Place, forecast: Forecast, unit: TemperatureUnit): String {
    val current = forecast.current
    val condition = WeatherCondition.fromWeatherCode(current.weatherCode)
    val today = forecast.daily.firstOrNull()
    return buildString {
        append(place.name).append(": ")
        append(condition.emoji(current.isDay)).append(' ')
        append(unit.format(current.temperatureC)).append(", ").append(condition.label)
        if (today != null) {
            append("\nEn yüksek ").append(unit.format(today.maxTemperatureC))
            append(" · En düşük ").append(unit.format(today.minTemperatureC))
        }
        append("\n— Clima")
    }
}

/** Android'in paylaşım menüsünü (share sheet) açar. Hangi uygulamayla paylaşılacağını kullanıcı seçer. */
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, null))
}
