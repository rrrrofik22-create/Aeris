package com.example.core

import com.example.core.ai.*
import com.example.core.automation.ActionRouter
import com.example.core.automation.IntentEngine
import com.example.core.automation.ShizukuEngine
import com.example.core.browser.BrowserProfileManager
import com.example.core.memory.ContextManager
import com.example.core.model.*
import com.example.core.planner.TaskPlanner
import com.example.core.tools.AgentToolSystem
import com.example.core.voice.VoiceManager
import com.example.data.local.AerisDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class AerisCore(
    val modelRouter: ModelRouter,
    val apiKeyManager: ApiKeyManager,
    val groqApiClient: GroqApiClient,
    val contextManager: ContextManager,
    val toolSystem: AgentToolSystem,
    val taskPlanner: TaskPlanner,
    val actionRouter: ActionRouter,
    val intentEngine: IntentEngine,
    val shizukuEngine: ShizukuEngine,
    val voiceManager: VoiceManager,
    val browserProfileManager: BrowserProfileManager,
    val database: AerisDatabase,
    val chatDao: ChatDao,
    val projectDao: ProjectDao
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _agentStatus = MutableStateFlow(AgentStatus.IDLE)
    val agentStatus: StateFlow<AgentStatus> = _agentStatus.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String>("session_default")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String>("Aeris Ready.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun switchSession(sessionId: String) {
        _activeSessionId.value = sessionId
    }

    fun setActiveProject(projectId: String?) {
        _activeProjectId.value = projectId
    }

    suspend fun createNewSession(title: String = "New Agent Mission"): String {
        val id = "chat_${UUID.randomUUID().toString().take(8)}"
        val session = ChatSessionEntity(
            id = id,
            title = title
        )
        chatDao.insertSession(session)
        _activeSessionId.value = id
        return id
    }

    fun processUserInput(
        input: String,
        attachmentPath: String? = null,
        attachmentType: String? = null,
        isVoiceInput: Boolean = false
    ) {
        scope.launch {
            val sessionId = _activeSessionId.value

            // 1. Record User Message
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = input,
                    attachmentPath = attachmentPath,
                    attachmentType = attachmentType
                )
            )

            _agentStatus.value = AgentStatus.THINKING
            _statusMessage.value = "Analyzing intent & assembling context..."

            // 2. Vision analysis if image attachment is present
            var visualContext: String? = null
            if (attachmentType == "image" && attachmentPath != null) {
                _agentStatus.value = AgentStatus.OBSERVING
                _statusMessage.value = "Running Qwen 3.8-27B Vision on screen image..."
                val visionResult = groqApiClient.analyzeImageWithVision(attachmentPath)
                visualContext = "Visual Analysis (Qwen 3.8-27B):\n${visionResult.visualDescription}\nDetected UI Elements: ${visionResult.detectedElements.joinToString()}"
            }

            // 3. Check if multi-step task planning is warranted
            val isComplexTask = input.contains("step", ignoreCase = true) ||
                    input.contains("create", ignoreCase = true) ||
                    input.contains("open", ignoreCase = true) ||
                    input.contains("fix", ignoreCase = true) ||
                    input.contains("and", ignoreCase = true) ||
                    input.contains("build", ignoreCase = true)

            var plan: TaskPlan? = null
            if (isComplexTask) {
                _agentStatus.value = AgentStatus.PLANNING
                _statusMessage.value = "Decomposing multi-step task plan..."
                plan = taskPlanner.createPlanFromGoal(input)
            }

            // 4. Assemble 8-layer Aeris context
            val assembled = contextManager.assembleContext(
                sessionId = sessionId,
                currentUserInput = input,
                activePlan = plan,
                activeProjectId = _activeProjectId.value,
                visualContext = visualContext,
                isMemoryEnabled = modelRouter.config.value.isMemoryEnabled
            )

            // 5. Call Primary AI Brain (GPT-OSS 120B)
            _agentStatus.value = AgentStatus.THINKING
            _statusMessage.value = "Consulting GPT-OSS 120B reasoning engine..."

            val apiResponse = groqApiClient.executeChatCompletion(
                messages = assembled.formattedHistory,
                tools = toolSystem.registeredTools,
                temperature = modelRouter.config.value.temperature
            )

            when (apiResponse) {
                is AgentApiResponse.TextSuccess -> {
                    _agentStatus.value = AgentStatus.SUCCESS
                    _statusMessage.value = "Completed."
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = apiResponse.content,
                            modelName = apiResponse.model
                        )
                    )
                    if (isVoiceInput) {
                        voiceManager.speak(apiResponse.content)
                    }
                    _agentStatus.value = AgentStatus.IDLE
                }

                is AgentApiResponse.ToolCallSuccess -> {
                    _agentStatus.value = AgentStatus.EXECUTING
                    _statusMessage.value = "Executing tool: ${apiResponse.toolName}..."

                    val toolResult = toolSystem.executeTool(
                        toolName = apiResponse.toolName,
                        arguments = apiResponse.arguments
                    )

                    _agentStatus.value = AgentStatus.VERIFYING
                    _statusMessage.value = "Verifying outcome..."

                    val report = buildString {
                        if (apiResponse.thought != null) {
                            appendLine(apiResponse.thought)
                            appendLine()
                        }
                        appendLine("Action: [${apiResponse.toolName}]")
                        appendLine("Result: ${toolResult.resultData}")
                        if (toolResult.verificationEvidence != null) {
                            appendLine("Verification: ${toolResult.verificationEvidence}")
                        }
                    }

                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = report,
                            modelName = apiResponse.model,
                            toolCallJson = apiResponse.toolName,
                            verificationEvidence = toolResult.verificationEvidence
                        )
                    )

                    if (isVoiceInput) {
                        voiceManager.speak("Executed ${apiResponse.toolName}. Result verified.")
                    }
                    _agentStatus.value = AgentStatus.IDLE
                    _statusMessage.value = "Task verified."
                }

                is AgentApiResponse.Error -> {
                    _agentStatus.value = AgentStatus.ERROR
                    _statusMessage.value = "Degraded mode active: ${apiResponse.message}"

                    val fallbackReply = if (apiResponse.message.contains("No active Groq API keys", ignoreCase = true)) {
                        "⚠️ Groq API key is required for live AI operations. Please open the Settings tab and configure one or more Groq API keys.\n\nIn the meantime, Aeris offline tools, projects, editor, and device automation actions remain fully accessible."
                    } else {
                        "Error encountered: ${apiResponse.message}\nAeris attempted automatic failover. Please check network or key health in Settings."
                    }

                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = fallbackReply,
                            modelName = "Aeris System"
                        )
                    )
                    _agentStatus.value = AgentStatus.IDLE
                }
            }
        }
    }
}
