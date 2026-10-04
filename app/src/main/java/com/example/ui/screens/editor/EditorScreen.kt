package com.example.ui.screens.editor

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.AerisCore
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectFileEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun EditorScreen(
    aerisCore: AerisCore,
    onNavigateToPreview: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val projectsFlow = remember { aerisCore.projectDao.getAllProjects() }
    val projects by projectsFlow.collectAsState(initial = emptyList())

    val activeProjectId by aerisCore.activeProjectId.collectAsState()
    val currentProject = projects.firstOrNull { it.id == activeProjectId } ?: projects.firstOrNull()

    LaunchedEffect(currentProject?.id) {
        if (activeProjectId == null && currentProject != null) {
            aerisCore.setActiveProject(currentProject.id)
        }
    }

    val projectFilesFlow = remember(currentProject?.id) {
        if (currentProject != null) {
            aerisCore.projectDao.getFilesForProject(currentProject.id)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
    val files by projectFilesFlow.collectAsState(initial = emptyList())

    var activeFileId by remember { mutableStateOf<String?>(null) }
    val activeFile = files.firstOrNull { it.id == activeFileId } ?: files.firstOrNull()

    LaunchedEffect(files) {
        if (activeFileId == null && files.isNotEmpty()) {
            activeFileId = files.first().id
        }
    }

    var codeContent by remember { mutableStateOf("") }
    LaunchedEffect(activeFile?.id) {
        codeContent = activeFile?.content.orEmpty()
    }

    var aiInstructionText by remember { mutableStateOf("") }
    var isGeneratingAiCode by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AerisDarkBackground)
            .testTag("editor_screen_container")
    ) {
        // Project Selector & Run Preview Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AerisDarkSurfaceVariant,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = AerisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = currentProject?.name ?: "Three.js Project",
                        color = AerisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AerisViolet.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentProject?.projectType ?: "THREE_JS",
                            color = AerisViolet,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Save Button
                    IconButton(
                        onClick = {
                            if (activeFile != null) {
                                coroutineScope.launch {
                                    aerisCore.projectDao.updateFile(activeFile.copy(content = codeContent))
                                    hasUnsavedChanges = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("editor_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save File",
                            tint = if (hasUnsavedChanges) AerisAmber else AerisTextMuted
                        )
                    }

                    // Run Preview
                    Button(
                        onClick = {
                            if (activeFile != null && hasUnsavedChanges) {
                                coroutineScope.launch {
                                    aerisCore.projectDao.updateFile(activeFile.copy(content = codeContent))
                                    hasUnsavedChanges = false
                                    onNavigateToPreview()
                                }
                            } else {
                                onNavigateToPreview()
                            }
                        },
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("editor_run_preview_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AerisEmerald),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = AerisDarkOnTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Run Preview",
                            color = AerisDarkOnTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // File Tabs Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(AerisObsidian)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(files, key = { it.id }) { file ->
                val isSelected = file.id == activeFile?.id
                Surface(
                    onClick = {
                        if (hasUnsavedChanges && activeFile != null) {
                            coroutineScope.launch {
                                aerisCore.projectDao.updateFile(activeFile.copy(content = codeContent))
                                hasUnsavedChanges = false
                            }
                        }
                        activeFileId = file.id
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) AerisDarkSurfaceVariant else Color.Transparent,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, AerisCyan.copy(alpha = 0.5f)) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when (file.fileExtension.lowercase()) {
                                "html" -> Icons.Default.Html
                                "js", "ts" -> Icons.Default.Javascript
                                "css" -> Icons.Default.Css
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = if (isSelected) AerisCyan else AerisTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = file.fileName,
                            color = if (isSelected) AerisTextPrimary else AerisTextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Code Editor Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF070A0F))
                .padding(8.dp)
        ) {
            TextField(
                value = codeContent,
                onValueChange = {
                    codeContent = it
                    hasUnsavedChanges = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("code_editor_text_field"),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFE2E8F0)
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        // AI Assistant Code Bar (interconnected with GPT-OSS 120B)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AerisDarkSurfaceVariant,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "AI Code Engine",
                        tint = AerisViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    TextField(
                        value = aiInstructionText,
                        onValueChange = { aiInstructionText = it },
                        placeholder = {
                            Text(
                                "Ask Aeris to modify code, add 3D effects, or optimize...",
                                color = AerisTextMuted,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_code_input_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = AerisObsidian,
                            unfocusedContainerColor = AerisObsidian,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = AerisTextPrimary,
                            unfocusedTextColor = AerisTextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (aiInstructionText.isNotBlank()) {
                                isGeneratingAiCode = true
                                val instruction = aiInstructionText.trim()
                                aiInstructionText = ""
                                coroutineScope.launch {
                                    val prompt = "Project File: ${activeFile?.fileName}\nCurrent Code:\n$codeContent\n\nTask: $instruction\n\nOutput only the updated complete valid code."
                                    val resp = aerisCore.groqApiClient.executeChatCompletion(
                                        messages = listOf(
                                            "system" to "You are Aeris AI Code Engine. Write pristine, working web / Three.js code without markdown wrap if possible.",
                                            "user" to prompt
                                        )
                                    )
                                    if (resp is com.example.core.ai.AgentApiResponse.TextSuccess) {
                                        val cleanCode = resp.content
                                            .replace(Regex("^```[a-zA-Z]*\\n"), "")
                                            .replace(Regex("\\n```$"), "")
                                        codeContent = cleanCode
                                        hasUnsavedChanges = true
                                        if (activeFile != null) {
                                            aerisCore.projectDao.updateFile(activeFile.copy(content = cleanCode))
                                        }
                                    }
                                    isGeneratingAiCode = false
                                }
                            }
                        },
                        enabled = aiInstructionText.isNotBlank() && !isGeneratingAiCode,
                        colors = ButtonDefaults.buttonColors(containerColor = AerisViolet),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("ai_code_generate_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        if (isGeneratingAiCode) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Generate", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
