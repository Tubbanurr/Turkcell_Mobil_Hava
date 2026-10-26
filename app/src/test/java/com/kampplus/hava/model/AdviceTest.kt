package com.kampplus.hava.model

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdviceTest {

    private val now = LocalDateTime.of(2026, 7, 14, 10, 0)

    private fun forecast(
        temp: Double = 22.0,
        feels: Double? = null,
        wind: Double? = null,
        humidity: Int? = null,
        hourly: List<HourlyForecast> = emptyList(),
        daily: List<DailyForecast> = emptyList()
    ) = Forecast(
        current = CurrentWeather(temp, 0, now, feels, humidity, wind, true),
        hourly = hourly,
        daily = daily
    )

    private fun hour(offset: Long, code: Int = 0, rain: Int? = 0, pressure: Double? = null, temp: Double = 20.0) =
        HourlyForecast(now.plusHours(offset), temp, code, rain, pressure)

    @Test
    fun `her zaman bir giyim onerisi vardir`() {
        val advice = forecast().buildAdvice()
        assertEquals("Hafif giyin", advice.last().title)
    }

    @Test
    fun `hissedilen sicakliga gore giyim secilir`() {
        assertEquals("Kalın mont giy", forecast(temp = 5.0, feels = -3.0).buildAdvice().last().title)
        assertEquals("Mont giy", forecast(feels = 8.0).buildAdvice().last().title)
        assertEquals("Çok sıcak", forecast(feels = 36.0).buildAdvice().last().title)
    }

    @Test
    fun `yagmur bekleniyorsa semsiye onerilir`() {
        val rainy = forecast(hourly = listOf(hour(0), hour(3, code = 61, rain = 80)))
        val umbrella = rainy.buildAdvice().first()
        assertEquals("Şemsiye al", umbrella.title)
        assertTrue(umbrella.detail.contains("13:00"))
    }

    @Test
    fun `kuru havada semsiye onerilmez`() {
        val dry = forecast(hourly = listOf(hour(0), hour(3)))
        assertTrue(dry.buildAdvice().none { it.title == "Şemsiye al" })
    }

    @Test
    fun `sert ruzgar uyarisi verilir`() {
        assertNotNull(forecast(wind = 55.0).buildAdvice().firstOrNull { it.title == "Rüzgâr sert" })
        assertNull(forecast(wind = 10.0).buildAdvice().firstOrNull { it.title == "Rüzgâr sert" })
    }

    @Test
    fun `hizli basinc dususu migren uyarisi uretir`() {
        val dropping = forecast(hourly = listOf(hour(0, pressure = 1015.0), hour(6, pressure = 1007.0)))
        assertNotNull(dropping.buildAdvice().firstOrNull { it.title == "Basınç değişimi" })
    }

    @Test
    fun `sabit basinc uyari uretmez`() {
        val steady = forecast(hourly = listOf(hour(0, pressure = 1013.0), hour(6, pressure = 1012.0)))
        assertNull(steady.buildAdvice().firstOrNull { it.title == "Basınç değişimi" })
    }

    @Test
    fun `yuksek uv ve buyuk sicaklik farki icin oneri gelir`() {
        val day = DailyForecast(LocalDate.of(2026, 7, 14), 12.0, 30.0, 0, uvIndexMax = 8.0)
        val titles = forecast(daily = listOf(day)).buildAdvice().map { it.title }
        assertTrue("Güneş kremi sür" in titles)
        assertTrue("Katmanlı giyin" in titles)
    }
}
