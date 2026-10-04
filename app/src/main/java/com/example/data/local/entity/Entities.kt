package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val summary: String? = null,
    val projectId: String? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelName: String? = null,
    val attachmentPath: String? = null,
    val attachmentType: String? = null, // "image", "audio", "file"
    val toolCallJson: String? = null,
    val verificationEvidence: String? = null,
    val planJson: String? = null
)

@Entity(tableName = "memory_items")
data class MemoryItemEntity(
    @PrimaryKey val id: String,
    val category: String, // "preference", "fact", "project", "instruction"
    val summary: String,
    val detail: String,
    val sourceChatId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val accessCount: Int = 0
)

@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey val id: String,
    val goal: String,
    val status: String, // "PENDING", "IN_PROGRESS", "VERIFYING", "COMPLETED", "FAILED"
    val currentStepIndex: Int = 0,
    val planJson: String,
    val finalReport: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "automation_workflows")
data class WorkflowEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val triggerType: String, // "MANUAL", "VOICE", "SCHEDULE", "EVENT"
    val stepsJson: String,
    val isEnabled: Boolean = true,
    val lastRunTimestamp: Long? = null,
    val lastRunStatus: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val projectType: String, // "WEB", "THREE_JS", "TOOL", "PROTOTYPE"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val previewUrl: String? = null
)

@Entity(tableName = "project_files")
data class ProjectFileEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val filePath: String,
    val fileName: String,
    val fileExtension: String,
    val content: String,
    val language: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey val id: String,
    val provider: String = "GROQ",
    val label: String,
    val apiKey: String,
    val status: String = "ACTIVE", // "ACTIVE", "COOLDOWN", "EXHAUSTED", "INVALID"
    val failureCount: Int = 0,
    val successCount: Int = 0,
    val cooldownUntilMillis: Long = 0,
    val lastUsedMillis: Long = 0
)

@Entity(tableName = "browser_profiles")
data class BrowserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val userAgent: String? = null,
    val isDefault: Boolean = false,
    val homeUrl: String = "https://www.google.com",
    val cookiesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
