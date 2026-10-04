package com.example.core.ai

import com.example.data.local.dao.ApiKeyDao
import com.example.data.local.entity.ApiKeyEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class KeyUsageResult(
    val key: String,
    val keyId: String
)

/**
 * Manages multiple API keys with automatic failover, cooldown, and health tracking.
 * Never exposes full plain-text keys in logs or exceptions.
 */
class ApiKeyManager(private val apiKeyDao: ApiKeyDao) {

    val allKeys: Flow<List<ApiKeyEntity>> = apiKeyDao.getAllKeys()

    suspend fun getNextAvailableKey(): KeyUsageResult? {
        val currentTime = System.currentTimeMillis()
        val usableKeys = apiKeyDao.getUsableKeys()

        // Find key that is ACTIVE or whose cooldown expired
        val readyKey = usableKeys.firstOrNull {
            it.status == "ACTIVE" || it.cooldownUntilMillis <= currentTime
        } ?: return null

        return KeyUsageResult(key = readyKey.apiKey, keyId = readyKey.id)
    }

    suspend fun recordKeyFailure(keyId: String, httpStatusCode: Int) {
        val cooldownDuration = when (httpStatusCode) {
            429 -> 60_000L // 1 minute cooldown for rate limit
            500, 502, 503, 504 -> 30_000L // 30 seconds for transient server error
            401 -> 24 * 3600_000L // 24 hours for invalid auth
            else -> 45_000L
        }
        val status = if (httpStatusCode == 401) "INVALID" else "COOLDOWN"
        val cooldownUntil = System.currentTimeMillis() + cooldownDuration
        apiKeyDao.markKeyCooldown(keyId, status, cooldownUntil)
    }

    suspend fun recordKeySuccess(keyId: String) {
        apiKeyDao.markKeySuccess(keyId, System.currentTimeMillis())
    }

    suspend fun addKey(label: String, rawApiKey: String): String {
        val cleanKey = rawApiKey.trim()
        val id = UUID.randomUUID().toString()
        val entity = ApiKeyEntity(
            id = id,
            provider = "GROQ",
            label = label.ifBlank { "Groq Key" },
            apiKey = cleanKey,
            status = "ACTIVE"
        )
        apiKeyDao.insertKey(entity)
        return id
    }

    suspend fun deleteKey(keyId: String) {
        apiKeyDao.deleteKey(keyId)
    }

    companion object {
        fun maskKey(key: String): String {
            if (key.length <= 8) return "••••••••"
            return key.take(4) + "••••••••" + key.takeLast(4)
        }
    }
}
