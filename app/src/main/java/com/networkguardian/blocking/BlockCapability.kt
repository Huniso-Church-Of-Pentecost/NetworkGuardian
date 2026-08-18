package com.networkguardian.blocking

import com.networkguardian.domain.models.CapabilityStatus

data class BlockCapability(
    val canRecordBlocklist: CapabilityStatus,   // local list-only tracking — always SUPPORTED
    val canEnforceBlock: CapabilityStatus,       // actual network-level enforcement
    val canPauseTemporarily: CapabilityStatus
)
