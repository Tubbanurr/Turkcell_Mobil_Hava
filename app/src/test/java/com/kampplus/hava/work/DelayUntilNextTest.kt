package com.kampplus.hava.work

import java.time.Duration
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DelayUntilNextTest {
    @Test
    fun `saat gelmediyse bugune planlanir`() {
        val delay = delayUntilNext(8, LocalDateTime.parse("2026-10-26T06:30"))
        assertEquals(Duration.ofMinutes(90), delay)
    }

    @Test
    fun `saat gectiyse yarina planlanir`() {
        val delay = delayUntilNext(8, LocalDateTime.parse("2026-10-26T09:00"))
        assertEquals(Duration.ofHours(23), delay)
    }
}
