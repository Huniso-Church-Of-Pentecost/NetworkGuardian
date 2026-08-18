package com.networkguardian.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {
    @Test
    fun `blank ip returns unavailable`() {
        assertEquals("Unavailable", unavailableIfBlank(null))
        assertEquals("Unavailable", unavailableIfBlank(""))
    }

    @Test
    fun `present value passes through`() {
        assertEquals("192.168.1.5", unavailableIfBlank("192.168.1.5"))
    }

    @Test
    fun `formatDuration handles hours and minutes`() {
        assertEquals("1h 5m", formatDuration(65 * 60_000L))
        assertEquals("5m", formatDuration(5 * 60_000L))
    }
}
