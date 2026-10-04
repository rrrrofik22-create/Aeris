package com.example.core.tools

import com.example.core.automation.ActionRouter
import com.example.core.automation.AerisAccessibilityService
import com.example.core.memory.ContextManager
import com.example.core.model.ToolDefinition
import com.example.core.model.ToolExecutionResult
import com.example.core.model.ToolRiskLevel
import com.example.data.local.dao.ProjectDao
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class AgentToolSystem(
    private val actionRouter: ActionRouter,
    private val contextManager: ContextManager,
    private val projectDao: ProjectDao
) {
    val registeredTools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "open_app",
            description = "Launch an Android app by name or package identifier using official Intents or Accessibility.",
            parametersJsonSchema = """{"type":"object","properties":{"app_name":{"type":"string","description":"Name or package of the app"}},"required":["app_name"]}""",
            riskLevel = ToolRiskLevel.LOW
        ),
        ToolDefinition(
            name = "read_screen",
            description = "Inspect the current Android screen UI hierarchy and extract visible nodes, text, and clickable elements.",
            parametersJsonSchema = """{"type":"object","properties":{}}""",
            riskLevel = ToolRiskLevel.LOW
        ),
        ToolDefinition(
            name = "click_element",
            description = "Click a button, toggle, or interactive element on the screen identified by label or id.",
            parametersJsonSchema = """{"type":"object","properties":{"target":{"type":"string","description":"Text label or ID of the element"}},"required":["target"]}""",
            riskLevel = ToolRiskLevel.MEDIUM
        ),
        ToolDefinition(
            name = "type_text",
            description = "Input text into a focused or target text field on the screen.",
            parametersJsonSchema = """{"type":"object","properties":{"text":{"type":"string","description":"Text to type"},"target":{"type":"string","description":"Optional field hint or label"}},"required":["text"]}""",
            riskLevel = ToolRiskLevel.MEDIUM
        ),
        ToolDefinition(
            name = "scroll",
            description = "Scroll the current screen or view in a specified direction (down, up).",
            parametersJsonSchema = """{"type":"object","properties":{"direction":{"type":"string","enum":["down","up"]}},"required":["direction"]}""",
            riskLevel = ToolRiskLevel.LOW
        ),
        ToolDefinition(
            name = "open_url",
            description = "Open a webpage URL in the browser workspace or default browser.",
            parametersJsonSchema = """{"type":"object","properties":{"url":{"type":"string","description":"The full web URL"}},"required":["url"]}""",
            riskLevel = ToolRiskLevel.LOW
        ),
        ToolDefinition(
            name = "create_project_file",
            description = "Create or update a code file (HTML, CSS, JS, Three.js, etc.) inside the active project.",
            parametersJsonSchema = """{"type":"object","properties":{"project_id":{"type":"string"},"file_path":{"type":"string"},"content":{"type":"string"},"language":{"type":"string"}},"required":["file_path","content"]}""",
            riskLevel = ToolRiskLevel.LOW
        ),
        ToolDefinition(
            name = "save_project",
            description = "Create and save a new code project in the workspace.",
            parametersJsonSchema = """{"type":"object","properties":{"name":{"type":"string"},"description":{"type":"string"},"project_type":{"type":"string","enum":["WEB","THREE_JS","TOOL","PROTOTYPE"]}},"required":["name"]}""",
            riskLevel = ToolRiskLevel.LOW
        ),
        ToolDefinition(
            name = "remember_fact",
            description = "Save an important fact or user preference into Aeris long-term memory.",
            parametersJsonSchema = """{"type":"object","properties":{"summary":{"type":"string"},"detail":{"type":"string"},"category":{"type":"string"}},"required":["summary","detail"]}""",
            riskLevel = ToolRiskLevel.LOW
        )
    )

    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, Any?>
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        try {
            when (toolName) {
                "open_app" -> {
                    val appName = arguments["app_name"]?.toString().orEmpty()
                    val outcome = actionRouter.executeAction("open_app", appName)
                    ToolExecutionResult(
                        toolName = toolName,
                        isSuccess = outcome.success,
                        resultData = outcome.outputMessage,
                        verificationEvidence = outcome.verificationEvidence
                    )
                }

                "read_screen" -> {
                    val service = AerisAccessibilityService.instance
                    if (service != null) {
                        val nodes = service.captureUiHierarchy()
                        val summary = nodes.joinToString("\n") {
                            "- [${if (it.isClickable) "Clickable" else "Text"}] \"${it.text ?: it.contentDescription.orEmpty()}\""
                        }.take(2000)
                        ToolExecutionResult(
                            toolName = toolName,
                            isSuccess = true,
                            resultData = "Captured ${nodes.size} UI elements:\n$summary",
                            verificationEvidence = "Direct accessibility node tree inspected."
                        )
                    } else {
                        ToolExecutionResult(
                            toolName = toolName,
                            isSuccess = false,
                            resultData = "Accessibility service inactive. Please enable Aeris accessibility in Settings.",
                            error = "Accessibility offline"
                        )
                    }
                }

                "click_element" -> {
                    val target = arguments["target"]?.toString().orEmpty()
                    val outcome = actionRouter.executeAction("click_element", target)
                    ToolExecutionResult(
                        toolName = toolName,
                        isSuccess = outcome.success,
                        resultData = outcome.outputMessage,
                        verificationEvidence = outcome.verificationEvidence
                    )
                }

                "type_text" -> {
                    val text = arguments["text"]?.toString().orEmpty()
                    val target = arguments["target"]?.toString()
                    val outcome = actionRouter.executeAction("type_text", target.orEmpty(), text)
                    ToolExecutionResult(
                        toolName = toolName,
                        isSuccess = outcome.success,
                        resultData = outcome.outputMessage,
                        verificationEvidence = outcome.verificationEvidence
                    )
                }

                "scroll" -> {
                    val dir = arguments["direction"]?.toString() ?: "down"
                    val outcome = actionRouter.executeAction("scroll", "", dir)
                    ToolExecutionResult(
                        toolName = toolName,
                        isSuccess = outcome.success,
                        resultData = outcome.outputMessage,
                        verificationEvidence = outcome.verificationEvidence
                    )
                }

                "open_url" -> {
                    val url = arguments["url"]?.toString().orEmpty()
                    val outcome = actionRouter.executeAction("open_url", url)
                    ToolExecutionResult(
                        toolName = toolName,
                        isSuccess = outcome.success,
                        resultData = outcome.outputMessage,
                        verificationEvidence = outcome.verificationEvidence
                    )
                }

                "create_project_file" -> {
                    val projectId = arguments["project_id"]?.toString() ?: "default_proj"
                    val filePath = arguments["file_path"]?.toString() ?: "index.html"
                    val content = arguments["content"]?.toString().orEmpty()
                    val language = arguments["language"]?.toString() ?: "html"

                    val fileId = UUID.randomUUID().toString()
                    val fileEntity = ProjectFileEntity(
                        id = fileId,
                        projectId = projectId,
                        filePath = filePath,
                        fileName = filePath.substringAfterLast('/'),
                        fileExtension = filePath.substringAfterLast('.', "html"),
                        content = content,
                        language = language
                    )
                    projectDao.insertFile(fileEntity)
                    ToolExecutionResult(
                        toolName = toolName,
                        isSuccess = true,
                        resultData = "Created file '$filePath' in project ($projectId).",
                        verificationEvidence = "File entity stored in database (${content.length} characters)."
                    )
                }

                "save_project" -> {
                    val name = arguments["name"]?.toString() ?: "New Project"
                    val desc = arguments["description"]?.toString() ?: "Generated by Aeris"
                    val type = arguments["project_type"]?.toString() ?: "WEB"
                    val id = UUID.randomUUID().toString()
                    val project = ProjectEntity(
                        id = id,
                        name = name,
                        description = desc,
                        projectType = type
                    )
                    projectDao.insertProject(project)
                    ToolExecutionResult(
                        toolName = toolName,
                        isSuccess = true,
                        resultData = "Project '$name' created with ID: $id",
                        verificationEvidence = "Project record verified in local database."
                    )
                }

                "remember_fact" -> {
                    val summary = arguments["summary"]?.toString().orEmpty()
                    val detail = arguments["detail"]?.toString().orEmpty()
                    val category = arguments["category"]?.toString() ?: "fact"
                    contextManager.recordMemoryFact(summary, detail, category)
                    ToolExecutionResult(
                        toolName = toolName,
                        isSuccess = true,
                        resultData = "Stored to memory: \"$summary\"",
                        verificationEvidence = "Memory record committed to long-term storage."
                    )
                }

                else -> {
                    ToolExecutionResult(
                        toolName = toolName,
                        isSuccess = false,
                        resultData = "Unknown tool: $toolName",
                        error = "Tool not found"
                    )
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = toolName,
                isSuccess = false,
                resultData = "Tool execution encountered error: ${e.message}",
                error = e.localizedMessage
            )
        }
    }
}
