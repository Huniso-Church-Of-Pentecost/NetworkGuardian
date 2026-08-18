package com.networkguardian.blocking

import com.networkguardian.data.repository.DeviceRepository
import com.networkguardian.domain.models.CapabilityStatus
import com.networkguardian.domain.models.OperationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockManagerTest {

    private lateinit var repository: DeviceRepository

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
    }

    @Test
    fun `blockDevice records blocklist entry with enforcement false when unsupported`() = runTest {
        val capability = BlockCapability(
            canRecordBlocklist = CapabilityStatus.SUPPORTED,
            canEnforceBlock = CapabilityStatus.NOT_AVAILABLE,
            canPauseTemporarily = CapabilityStatus.NOT_AVAILABLE
        )
        val manager = BlockManager(repository, capability)

        val result = manager.blockDevice(
            deviceId = "aa:bb", macAddress = "aa:bb:cc:dd:ee:ff", ipAddress = "192.168.1.5",
            label = "Test Device", profileId = "profile1", reason = null, nowEpochMs = 1000L
        )

        assertTrue(result is OperationResult.Success)
        coVerify {
            repository.recordBlock(match {
                it.enforcementActive == false &&
                    it.enforcementNote.contains("not supported", ignoreCase = true)
            })
        }
    }

    @Test
    fun `blockDevice returns unsupported when blocklist storage unavailable`() = runTest {
        val capability = BlockCapability(
            canRecordBlocklist = CapabilityStatus.NOT_AVAILABLE,
            canEnforceBlock = CapabilityStatus.NOT_AVAILABLE,
            canPauseTemporarily = CapabilityStatus.NOT_AVAILABLE
        )
        val manager = BlockManager(repository, capability)

        val result = manager.blockDevice(
            deviceId = "id", macAddress = null, ipAddress = null,
            label = "Device", profileId = "p1", reason = null, nowEpochMs = 0L
        )

        assertTrue(result is OperationResult.Unsupported)
    }

    @Test
    fun `pauseDevice returns unsupported when platform cannot pause`() = runTest {
        val capability = BlockCapability(
            canRecordBlocklist = CapabilityStatus.SUPPORTED,
            canEnforceBlock = CapabilityStatus.NOT_AVAILABLE,
            canPauseTemporarily = CapabilityStatus.NOT_AVAILABLE
        )
        val manager = BlockManager(repository, capability)

        val result = manager.pauseDevice("id", "Device", "p1", 15, 0L)

        assertTrue(result is OperationResult.Unsupported)
    }

    @Test
    fun `unblockDevice always succeeds locally`() = runTest {
        val capability = BlockCapability(CapabilityStatus.SUPPORTED, CapabilityStatus.NOT_AVAILABLE, CapabilityStatus.NOT_AVAILABLE)
        val manager = BlockManager(repository, capability)

        val result = manager.unblockDevice("id", "Device", "p1", 0L)

        assertEquals(OperationResult.Success, result)
        coVerify { repository.recordUnblock("id") }
    }
}
