package com.networkguardian.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NetworkProfileEntity::class,
        DeviceEntity::class,
        TrustedDeviceEntity::class,
        BlockedDeviceEntity::class,
        PausedDeviceEntity::class,
        ConnectionEventEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NetworkGuardianDatabase : RoomDatabase() {
    abstract fun networkProfileDao(): NetworkProfileDao
    abstract fun deviceDao(): DeviceDao
    abstract fun trustedDeviceDao(): TrustedDeviceDao
    abstract fun blockedDeviceDao(): BlockedDeviceDao
    abstract fun pausedDeviceDao(): PausedDeviceDao
    abstract fun connectionEventDao(): ConnectionEventDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile private var INSTANCE: NetworkGuardianDatabase? = null

        // Reserved for future schema changes. Room's default behavior throws if a migration
        // is missing, which is intentional: we never silently wipe user data on upgrade.
        // Add Migration(1, 2) etc. here as the schema evolves.
        private val MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf()

        fun getInstance(context: Context): NetworkGuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetworkGuardianDatabase::class.java,
                    "networkguardian.db"
                ).addMigrations(*MIGRATIONS).build().also { INSTANCE = it }
            }
        }
    }
}
