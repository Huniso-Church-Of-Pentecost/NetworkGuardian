package com.networkguardian.blocking

import com.networkguardian.domain.models.OperationResult

/**
 * Abstraction for anything that can actually enforce a network-level block against a device
 * (kick a client, deny future association). This lets router support be added later without
 * touching BlockManager or the UI.
 *
 * There is intentionally only ONE implementation shipped today: AndroidHotspotController,
 * which is honest that it cannot enforce anything (see its doc comment). A RouterController
 * is NOT implemented in this build because doing so would require either (a) a specific
 * router vendor's official local admin API/SDK, which the user would need to configure with
 * their own router's credentials, or (b) unauthorized access to router internals, which this
 * project refuses to do. Adding real router support means implementing this interface against
 * a specific vendor's documented API — see README "Known Limitations" for how to extend this.
 */
interface NetworkController {
    suspend fun enforceBlock(macAddress: String?, ipAddress: String?): OperationResult
    suspend fun removeBlock(macAddress: String?, ipAddress: String?): OperationResult
    fun supportsEnforcement(): Boolean
}

/**
 * The only controller wired up today. It never claims to enforce anything, matching the
 * reality that stock, non-privileged Android exposes no public API for a third-party app to
 * kick or ban a hotspot client.
 */
class AndroidHotspotController : NetworkController {
    override suspend fun enforceBlock(macAddress: String?, ipAddress: String?): OperationResult =
        OperationResult.Unsupported(
            "Blocking is not supported on this device/network through the available Android APIs."
        )

    override suspend fun removeBlock(macAddress: String?, ipAddress: String?): OperationResult =
        OperationResult.Unsupported(
            "This device/network does not support enforced unblocking through the available Android APIs."
        )

    override fun supportsEnforcement(): Boolean = false
}

/**
 * Placeholder extension point for a future vendor-specific implementation. Left unimplemented
 * on purpose — see NetworkController doc comment. Do not wire this into DI until a real,
 * documented vendor API integration exists.
 */
class RouterController : NetworkController {
    override suspend fun enforceBlock(macAddress: String?, ipAddress: String?): OperationResult =
        OperationResult.Unsupported("Router integration is not configured for this network profile.")

    override suspend fun removeBlock(macAddress: String?, ipAddress: String?): OperationResult =
        OperationResult.Unsupported("Router integration is not configured for this network profile.")

    override fun supportsEnforcement(): Boolean = false
}
