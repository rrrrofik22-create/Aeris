package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items ORDER BY accessCount DESC, timestamp DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int): List<MemoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(item: MemoryItemEntity)

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemories()

    @Query("UPDATE memory_items SET accessCount = accessCount + 1 WHERE id = :id")
    suspend fun incrementAccessCount(id: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM agent_tasks ORDER BY updatedAt DESC")
    fun getAllTasks(): Flow<List<AgentTaskEntity>>

    @Query("SELECT * FROM agent_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): AgentTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AgentTaskEntity)

    @Update
    suspend fun updateTask(task: AgentTaskEntity)

    @Query("DELETE FROM agent_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)
}

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM automation_workflows ORDER BY createdAt DESC")
    fun getAllWorkflows(): Flow<List<WorkflowEntity>>

    @Query("SELECT * FROM automation_workflows WHERE id = :id LIMIT 1")
    suspend fun getWorkflowById(id: String): WorkflowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: WorkflowEntity)

    @Update
    suspend fun updateWorkflow(workflow: WorkflowEntity)

    @Query("DELETE FROM automation_workflows WHERE id = :id")
    suspend fun deleteWorkflow(id: String)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY filePath ASC")
    fun getFilesForProject(projectId: String): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE id = :fileId LIMIT 1")
    suspend fun getFileById(fileId: String): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity)

    @Update
    suspend fun updateFile(file: ProjectFileEntity)

    @Query("DELETE FROM project_files WHERE id = :fileId")
    suspend fun deleteFile(fileId: String)

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteFilesForProject(projectId: String)
}

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY lastUsedMillis ASC")
    fun getAllKeys(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys WHERE status != 'INVALID' ORDER BY cooldownUntilMillis ASC, failureCount ASC")
    suspend fun getUsableKeys(): List<ApiKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: ApiKeyEntity)

    @Update
    suspend fun updateKey(key: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE id = :id")
    suspend fun deleteKey(id: String)

    @Query("UPDATE api_keys SET status = :status, cooldownUntilMillis = :cooldownUntil, failureCount = failureCount + 1 WHERE id = :id")
    suspend fun markKeyCooldown(id: String, status: String, cooldownUntil: Long)

    @Query("UPDATE api_keys SET status = 'ACTIVE', failureCount = 0, successCount = successCount + 1, lastUsedMillis = :timestamp WHERE id = :id")
    suspend fun markKeySuccess(id: String, timestamp: Long)
}

@Dao
interface BrowserProfileDao {
    @Query("SELECT * FROM browser_profiles ORDER BY isDefault DESC, createdAt ASC")
    fun getAllProfiles(): Flow<List<BrowserProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BrowserProfileEntity)

    @Query("DELETE FROM browser_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}
