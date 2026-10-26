package com.kampplus.hava.model

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyTest {

    @Test
    fun `bilinen yeni ay ve dolunay dogru hesaplanir`() {
        // 11 Ocak 2024 yeni ay, 25 Ocak 2024 dolunay.
        assertTrue(moonPhaseOn(LocalDate.of(2024, 1, 11)).illumination < 0.05)
        assertTrue(moonPhaseOn(LocalDate.of(2024, 1, 25)).illumination > 0.95)
        assertEquals("Dolunay", moonPhaseOn(LocalDate.of(2024, 1, 25)).name)
    }

    @Test
    fun `en yakin meteor yagmuru bulunur`() {
        val shower = nextMeteorShower(LocalDate.of(2026, 8, 1))
        assertEquals(MeteorShower.PERSEIDS, shower?.shower)
        assertEquals(11L, shower?.daysUntil)
    }

    @Test
    fun `yil donumunde meteor yagmuru sonraki yila bakar`() {
        assertEquals(MeteorShower.QUADRANTIDS, nextMeteorShower(LocalDate.of(2026, 12, 25))?.shower)
    }

    @Test
    fun `yaklasan yagmur yoksa null doner`() {
        assertNull(nextMeteorShower(LocalDate.of(2026, 2, 1)))
    }

    private fun forecastWithClouds(cloud: Int): Forecast {
        val now = LocalDateTime.of(2026, 7, 14, 18, 0)
        val hours = List(24) { HourlyForecast(now.plusHours(it.toLong()), 15.0, 0, 0, cloudCoverPercent = cloud) }
        return Forecast(CurrentWeather(15.0, 0, now), hours, emptyList())
    }

    @Test
    fun `acik gokyuzu bulutlu gokten daha iyi puan alir`() {
        val clear = forecastWithClouds(0).stargazingTonight()
        val cloudy = forecastWithClouds(100).stargazingTonight()
        assertNotNull(clear)
        assertTrue(clear!!.score > cloudy!!.score)
    }

    @Test
    fun `bulutluluk verisi yoksa null doner`() {
        val now = LocalDateTime.of(2026, 7, 14, 18, 0)
        val forecast = Forecast(CurrentWeather(15.0, 0, now), listOf(HourlyForecast(now, 15.0, 0)), emptyList())
        assertNull(forecast.stargazingTonight())
    }
}
