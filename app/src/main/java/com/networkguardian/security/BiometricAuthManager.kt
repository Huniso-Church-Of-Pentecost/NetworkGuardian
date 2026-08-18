package com.networkguardian.security

import androidx.biometric.BiometricManager as AndroidXBiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    UNKNOWN
}

/** Thin wrapper around androidx.biometric — the documented, supported biometric API. */
class BiometricAuthManager(private val activity: FragmentActivity) {

    fun availability(): BiometricAvailability {
        val manager = AndroidXBiometricManager.from(activity)
        return when (manager.canAuthenticate(AndroidXBiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            AndroidXBiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            AndroidXBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            AndroidXBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            AndroidXBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNKNOWN
        }
    }

    fun authenticate(
        executor: Executor,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onFailed()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock NetworkGuardian")
            .setSubtitle("Confirm your identity to access network controls")
            .setAllowedAuthenticators(AndroidXBiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Use PIN instead")
            .build()

        prompt.authenticate(info)
    }
}
