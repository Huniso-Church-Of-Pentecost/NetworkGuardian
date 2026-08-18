package com.networkguardian.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkProfileDao {
    @Query("SELECT * FROM network_profiles ORDER BY lastActiveEpochMs DESC")
    fun observeProfiles(): Flow<List<NetworkProfileEntity>>

    @Query("SELECT * FROM network_profiles WHERE id = :id")
    suspend fun getProfile(id: String): NetworkProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: NetworkProfileEntity)

    @Delete
    suspend fun delete(profile: NetworkProfileEntity)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE networkProfileId = :profileId ORDER BY lastSeenEpochMs DESC")
    fun observeDevices(profileId: String): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getDevice(id: String): DeviceEntity?

    @Query("SELECT * FROM devices WHERE id = :id")
    fun observeDevice(id: String): Flow<DeviceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity)

    @Update
    suspend fun update(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun forget(id: String)
}

@Dao
interface TrustedDeviceDao {
    @Query("SELECT * FROM trusted_devices")
    fun observeAll(): Flow<List<TrustedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun trust(entity: TrustedDeviceEntity)

    @Query("DELETE FROM trusted_devices WHERE deviceId = :deviceId")
    suspend fun untrust(deviceId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM trusted_devices WHERE deviceId = :deviceId)")
    suspend fun isTrusted(deviceId: String): Boolean
}

@Dao
interface BlockedDeviceDao {
    @Query("SELECT * FROM blocked_devices ORDER BY blockedAtEpochMs DESC")
    fun observeAll(): Flow<List<BlockedDeviceEntity>>

    @Query("SELECT * FROM blocked_devices WHERE deviceId = :deviceId")
    suspend fun get(deviceId: String): BlockedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun block(entity: BlockedDeviceEntity)

    @Query("DELETE FROM blocked_devices WHERE deviceId = :deviceId")
    suspend fun unblock(deviceId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_devices WHERE deviceId = :deviceId)")
    suspend fun isBlocked(deviceId: String): Boolean
}

@Dao
interface PausedDeviceDao {
    @Query("SELECT * FROM paused_devices")
    fun observeAll(): Flow<List<PausedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun pause(entity: PausedDeviceEntity)

    @Query("DELETE FROM paused_devices WHERE deviceId = :deviceId")
    suspend fun resume(deviceId: String)

    @Query("SELECT * FROM paused_devices WHERE resumeAtEpochMs IS NOT NULL AND resumeAtEpochMs <= :nowEpochMs")
    suspend fun expiredPauses(nowEpochMs: Long): List<PausedDeviceEntity>
}

@Dao
interface ConnectionEventDao {
    @Query("SELECT * FROM connection_events WHERE networkProfileId = :profileId ORDER BY timestampEpochMs DESC LIMIT :limit")
    fun observeRecent(profileId: String, limit: Int = 200): Flow<List<ConnectionEventEntity>>

    @Insert
    suspend fun insert(event: ConnectionEventEntity)

    @Query("DELETE FROM connection_events WHERE networkProfileId = :profileId")
    suspend fun clearHistory(profileId: String)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun get(key: String): AppSettingsEntity?

    @Query("SELECT * FROM app_settings")
    fun observeAll(): Flow<List<AppSettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: AppSettingsEntity)
}
