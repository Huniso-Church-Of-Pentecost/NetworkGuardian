package com.networkguardian.security

import com.networkguardian.data.repository.SettingsRepository

enum class AutoLockOption(val minutes: Int) {
    IMMEDIATELY(0),
    ONE_MINUTE(1),
    FIVE_MINUTES(5),
    FIFTEEN_MINUTES(15),
    NEVER(-1)
}

/**
 * Coordinates PIN setup/verification and auto-lock timing. Biometric prompts themselves are
 * handled by BiometricManager (Android's BiometricPrompt); this class owns the PIN fallback
 * and the "should the app be locked right now" decision.
 */
class AppLockManager(private val settings: SettingsRepository) {

    suspend fun isPinSet(): Boolean = settings.getString(SettingsRepository.Keys.PIN_HASH) != null

    suspend fun setPin(pin: String) {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(pin, salt)
        settings.setString(SettingsRepository.Keys.PIN_SALT, salt)
        settings.setString(SettingsRepository.Keys.PIN_HASH, hash)
    }

    suspend fun verifyPin(pin: String): Boolean {
        val salt = settings.getString(SettingsRepository.Keys.PIN_SALT) ?: return false
        val hash = settings.getString(SettingsRepository.Keys.PIN_HASH) ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    suspend fun clearPin() {
        settings.setString(SettingsRepository.Keys.PIN_HASH, "")
        settings.setString(SettingsRepository.Keys.PIN_SALT, "")
    }

    suspend fun autoLockOption(): AutoLockOption {
        val minutes = settings.getInt(SettingsRepository.Keys.AUTO_LOCK_MINUTES, AutoLockOption.FIVE_MINUTES.minutes)
        return AutoLockOption.values().firstOrNull { it.minutes == minutes } ?: AutoLockOption.FIVE_MINUTES
    }

    suspend fun setAutoLockOption(option: AutoLockOption) {
        settings.setInt(SettingsRepository.Keys.AUTO_LOCK_MINUTES, option.minutes)
    }

    fun shouldLock(lastBackgroundedEpochMs: Long, nowEpochMs: Long, option: AutoLockOption): Boolean {
        if (option == AutoLockOption.NEVER) return false
        val elapsedMinutes = (nowEpochMs - lastBackgroundedEpochMs) / 60_000.0
        return elapsedMinutes >= option.minutes
    }
}
