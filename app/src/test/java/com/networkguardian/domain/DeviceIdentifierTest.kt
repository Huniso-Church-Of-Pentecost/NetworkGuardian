package com.networkguardian.domain

import com.networkguardian.domain.models.DeviceType
import com.networkguardian.network.identification.DeviceIdentifier
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceIdentifierTest {
    @Test
    fun `null hostname yields unknown`() {
        assertEquals(DeviceType.UNKNOWN, DeviceIdentifier.classify(null))
    }

    @Test
    fun `iphone hostname classifies as iphone`() {
        assertEquals(DeviceType.IPHONE, DeviceIdentifier.classify("Johns-iPhone"))
    }

    @Test
    fun `unrecognized hostname yields unknown rather than guessing`() {
        assertEquals(DeviceType.UNKNOWN, DeviceIdentifier.classify("device-4471"))
    }

    @Test
    fun `iot keyword classifies as iot device`() {
        assertEquals(DeviceType.IOT_DEVICE, DeviceIdentifier.classify("sonoff-plug-01"))
    }
}
