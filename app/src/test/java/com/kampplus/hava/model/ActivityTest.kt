package com.kampplus.hava.model

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTest {

    private val start = LocalDateTime.of(2026, 7, 14, 6, 0)

    /** İki günlük saatlik tahmin; [rainyDay] verilen günün tamamı yağmurlu. */
    private fun forecast(rainyDay: LocalDate? = null, temp: Double = 15.0): Forecast {
        val hours = List(48) { i ->
            val time = start.plusHours(i.toLong())
            val rainy = time.toLocalDate() == rainyDay
            HourlyForecast(time, temp, if (rainy) 63 else 0, if (rainy) 90 else 0, 1013.0, 40, 5.0)
        }
        val days = (0..1).map {
            val date = start.toLocalDate().plusDays(it.toLong())
            DailyForecast(date, 10.0, 20.0, 0, sunrise = date.atTime(5, 30), sunset = date.atTime(20, 30))
        }
        return Forecast(CurrentWeather(temp, 0, start), hours, days)
    }

    @Test
    fun `yagmursuz ve ilik gunde kosu yuksek puan alir`() {
        val best = forecast().planActivity(Activity.RUNNING).first()
        assertTrue("Puan düşük: ${best.score}", best.score >= 80)
    }

    @Test
    fun `yagmurlu gun planin sonuna duser`() {
        val rainyDay = start.toLocalDate()
        val slots = forecast(rainyDay = rainyDay).planActivity(Activity.RUNNING)
        assertEquals(start.toLocalDate().plusDays(1), slots.first().time.toLocalDate())
        assertTrue(slots.first().score > slots.last().score)
    }

    @Test
    fun `her gun icin en fazla bir oneri doner`() {
        val slots = forecast().planActivity(Activity.CAR_WASH, limit = 5)
        assertEquals(slots.map { it.time.toLocalDate() }.distinct().size, slots.size)
    }

    @Test
    fun `fotograf yalnizca gun dogumu ve batimi civarinda onerilir`() {
        val slot = forecast().planActivity(Activity.PHOTOGRAPHY).first()
        assertTrue(slot.time.hour in listOf(4, 5, 6, 7, 19, 20, 21))
    }

    @Test
    fun `mangal aksam saatlerinde onerilir`() {
        assertTrue(forecast(temp = 24.0).planActivity(Activity.BARBECUE).all { it.time.hour in 16..21 })
    }

    @Test
    fun `puan etiketi dogru`() {
        assertEquals("Harika", ActivitySlot(start, 90, 20.0, 0).verdict)
        assertEquals("Zayıf", ActivitySlot(start, 10, 20.0, 0).verdict)
    }
}
