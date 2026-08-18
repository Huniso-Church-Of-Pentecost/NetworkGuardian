package com.networkguardian.domain.models

/**
 * Every network-control operation (block, unblock, pause, hotspot control) must return one
 * of these outcomes. The UI reflects the real result — NetworkGuardian never shows success
 * for an operation whose outcome is unknown or unsupported.
 */
sealed class OperationResult {
    data object Success : OperationResult()
    data class Failed(val reason: String) : OperationResult()
    data class Unsupported(val explanation: String) : OperationResult()
    data class PermissionDenied(val permission: String) : OperationResult()
}
