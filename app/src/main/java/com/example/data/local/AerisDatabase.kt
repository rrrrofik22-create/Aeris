package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        MemoryItemEntity::class,
        AgentTaskEntity::class,
        WorkflowEntity::class,
        ProjectEntity::class,
        ProjectFileEntity::class,
        ApiKeyEntity::class,
        BrowserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AerisDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao
    abstract fun taskDao(): TaskDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun projectDao(): ProjectDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun browserProfileDao(): BrowserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AerisDatabase? = null

        fun getInstance(context: Context): AerisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AerisDatabase::class.java,
                    "aeris_agent_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
