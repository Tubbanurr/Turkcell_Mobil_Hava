package com.kampplus.hava.model

import com.kampplus.hava.data.AirQualityCurrent
import com.kampplus.hava.data.AirQualityResponse
import com.kampplus.hava.data.toAirQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirQualityTest {

    @Test
    fun `indeks sinirlari dogru seviyeye cevrilir`() {
        assertEquals(AirQualityLevel.VERY_GOOD, AirQualityLevel.fromAqi(10))
        assertEquals(AirQualityLevel.MODERATE, AirQualityLevel.fromAqi(50))
        assertEquals(AirQualityLevel.EXTREMELY_POOR, AirQualityLevel.fromAqi(140))
    }

    @Test
    fun `yanit polen ve toz ile modele cevrilir`() {
        val response = AirQualityResponse(AirQualityCurrent(europeanAqi = 42.0, pm25 = 9.5, grass = 30.0, dust = 12.0))
        val air = response.toAirQuality()!!

        assertEquals(42, air.europeanAqi)
        assertEquals(12.0, air.dust!!, 0.01)
        assertEquals("Orta", air.pollen.single().level)
    }

    @Test
    fun `indeks yoksa kart gosterilmez`() {
        assertNull(AirQualityResponse(AirQualityCurrent()).toAirQuality())
        assertNull(AirQualityResponse(null).toAirQuality())
    }
}
