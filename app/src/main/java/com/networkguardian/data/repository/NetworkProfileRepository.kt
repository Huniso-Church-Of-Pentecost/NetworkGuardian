package com.networkguardian.data.repository

import com.networkguardian.data.database.NetworkProfileDao
import com.networkguardian.data.database.NetworkProfileEntity

class NetworkProfileRepository(private val dao: NetworkProfileDao) {
    fun observeProfiles() = dao.observeProfiles()
    suspend fun getProfile(id: String) = dao.getProfile(id)
    suspend fun upsertProfile(entity: NetworkProfileEntity) = dao.upsert(entity)
    suspend fun deleteProfile(entity: NetworkProfileEntity) = dao.delete(entity)
}
