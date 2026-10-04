package com.example.core.model

/**
 * Centralized Model Router Configuration.
 * Model names and providers are NEVER hardcoded throughout the codebase.
 */
data class ModelConfiguration(
    val mainModel: String = "openai/gpt-oss-120b",
    val visionModel: String = "qwen/qwen3.8-27b",
    val sttModel: String = "whisper-large-v3-turbo",
    val ttsVoice: String = "Default Natural",
    val embeddingModel: String = "text-embedding-3-small",
    val temperature: Float = 0.6f,
    val maxTokens: Int = 4096,
    val isAutoVerificationEnabled: Boolean = true,
    val isMemoryEnabled: Boolean = true
)

enum class AgentStatus {
    IDLE,
    LISTENING,
    THINKING,
    PLANNING,
    EXECUTING,
    OBSERVING,
    VERIFYING,
    SPEAKING,
    WAITING_CONFIRMATION,
    SUCCESS,
    RECOVERING,
    ERROR
}

enum class ToolRiskLevel {
    LOW,        // Execute automatically
    MEDIUM,     // Configurable confirmation
    HIGH        // Explicit user confirmation required
}

data class ToolDefinition(
    val name: String,
    val description: String,
    val parametersJsonSchema: String,
    val riskLevel: ToolRiskLevel = ToolRiskLevel.LOW,
    val requiredPermission: String? = null
)

data class ToolInvocation(
    val toolName: String,
    val arguments: Map<String, Any?>
)

data class ToolExecutionResult(
    val toolName: String,
    val isSuccess: Boolean,
    val resultData: String,
    val verificationEvidence: String? = null,
    val error: String? = null
)

data class PlanStep(
    val id: String,
    val order: Int,
    val title: String,
    val description: String,
    val toolName: String? = null,
    val toolArgs: Map<String, String> = emptyMap(),
    var status: StepStatus = StepStatus.PENDING,
    var observation: String? = null,
    var verificationResult: String? = null,
    var retryCount: Int = 0
)

enum class StepStatus {
    PENDING,
    IN_PROGRESS,
    VERIFYING,
    COMPLETED,
    FAILED,
    RECOVERING,
    SKIPPED
}

data class TaskPlan(
    val taskId: String,
    val goal: String,
    val steps: List<PlanStep>,
    var currentStepIndex: Int = 0,
    var isCompleted: Boolean = false,
    var isFailed: Boolean = false,
    var finalReport: String? = null
)
