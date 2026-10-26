package com.kampplus.hava.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TemperatureUnitTest {
    @Test
    fun `celsius degismeden yazilir`() = assertEquals("21°", TemperatureUnit.CELSIUS.format(21.4))

    @Test
    fun `fahrenheit dogru cevrilir`() {
        assertEquals(32.0, TemperatureUnit.FAHRENHEIT.convert(0.0), 0.001)
        assertEquals("212°", TemperatureUnit.FAHRENHEIT.format(100.0))
    }
}
