package com.example.core.memory

import com.example.core.model.TaskPlan
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

data class AssembledContext(
    val systemPrompt: String,
    val formattedHistory: List<Pair<String, String>>, // role to content
    val retrievedMemories: List<MemoryItemEntity>,
    val activeProject: ProjectEntity?,
    val visualContext: String?
)

/**
 * 8-Layer Memory & Context Engine.
 * Belongs exclusively to Aeris across model switches.
 */
class ContextManager(
    private val memoryDao: MemoryDao,
    private val chatDao: ChatDao,
    private val projectDao: ProjectDao
) {
    suspend fun assembleContext(
        sessionId: String,
        currentUserInput: String,
        activeTaskId: String? = null,
        activePlan: TaskPlan? = null,
        activeProjectId: String? = null,
        visualContext: String? = null,
        isMemoryEnabled: Boolean = true
    ): AssembledContext {
        // 1. Retrieve persistent memories
        val retrievedMemories = if (isMemoryEnabled) {
            memoryDao.getTopMemories(limit = 6)
        } else emptyList()

        // 2. Retrieve active project context if set
        val project = if (activeProjectId != null) {
            projectDao.getProjectById(activeProjectId)
        } else null

        val projectFiles = if (activeProjectId != null) {
            projectDao.getFilesForProject(activeProjectId).firstOrNull() ?: emptyList()
        } else emptyList()

        // 3. Retrieve recent conversation messages (sliding window)
        val allMessages = chatDao.getMessagesForSession(sessionId).firstOrNull() ?: emptyList()
        val recentMessages = allMessages.takeLast(10) // Avoid blowing context window

        // 4. Build System Prompt with Aeris Identity and Retrieved Context
        val systemPrompt = buildString {
            appendLine("You are Aeris, an autonomous, professional Android AI Agent workspace.")
            appendLine("You are driven by the primary reasoning brain: GPT-OSS 120B.")
            appendLine("Core Agent Loop: UNDERSTAND -> PLAN -> SELECT TOOLS -> EXECUTE -> OBSERVE -> VERIFY -> RECOVER -> RESPOND.")
            appendLine("Never assume an action succeeded without verification.")
            appendLine("Be precise, actionable, confident, and professional.")

            if (retrievedMemories.isNotEmpty()) {
                appendLine("\n--- AERIS LONG-TERM MEMORY ---")
                for (mem in retrievedMemories) {
                    appendLine("• [${mem.category.uppercase()}] ${mem.summary}: ${mem.detail}")
                }
            }

            if (project != null) {
                appendLine("\n--- ACTIVE PROJECT CONTEXT ---")
                appendLine("Project: ${project.name} (${project.projectType})")
                appendLine("Description: ${project.description}")
                if (projectFiles.isNotEmpty()) {
                    appendLine("Files in project:")
                    for (file in projectFiles) {
                        appendLine("  - ${file.filePath} (${file.language}, ${file.content.length} chars)")
                    }
                }
            }

            if (visualContext != null && visualContext.isNotBlank()) {
                appendLine("\n--- QWEN VISION SCREEN CONTEXT ---")
                appendLine(visualContext)
            }

            if (activePlan != null && !activePlan.isCompleted) {
                appendLine("\n--- CURRENT TASK EXECUTION PLAN ---")
                appendLine("Goal: ${activePlan.goal}")
                appendLine("Current Step Index: ${activePlan.currentStepIndex + 1}/${activePlan.steps.size}")
                for (step in activePlan.steps) {
                    val statusMarker = when (step.status) {
                        com.example.core.model.StepStatus.COMPLETED -> "[DONE]"
                        com.example.core.model.StepStatus.IN_PROGRESS -> "[RUNNING]"
                        com.example.core.model.StepStatus.VERIFYING -> "[VERIFYING]"
                        com.example.core.model.StepStatus.FAILED -> "[FAILED]"
                        com.example.core.model.StepStatus.PENDING -> "[PENDING]"
                        else -> "[STATUS: ${step.status}]"
                    }
                    appendLine("$statusMarker Step ${step.order}: ${step.title} -> ${step.description}")
                }
            }
        }

        // Format history
        val formattedHistory = mutableListOf<Pair<String, String>>()
        formattedHistory.add("system" to systemPrompt)
        for (msg in recentMessages) {
            formattedHistory.add(msg.role to msg.content)
        }
        formattedHistory.add("user" to currentUserInput)

        return AssembledContext(
            systemPrompt = systemPrompt,
            formattedHistory = formattedHistory,
            retrievedMemories = retrievedMemories,
            activeProject = project,
            visualContext = visualContext
        )
    }

    suspend fun recordMemoryFact(summary: String, detail: String, category: String = "fact", sourceChatId: String? = null) {
        val memory = MemoryItemEntity(
            id = UUID.randomUUID().toString(),
            category = category,
            summary = summary,
            detail = detail,
            sourceChatId = sourceChatId
        )
        memoryDao.insertMemory(memory)
    }

    suspend fun forgetMemory(id: String) {
        memoryDao.deleteMemory(id)
    }

    suspend fun clearAllMemory() {
        memoryDao.clearAllMemories()
    }
}
