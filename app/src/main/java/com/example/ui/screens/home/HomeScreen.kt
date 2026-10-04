package com.example.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.core.model.AgentStatus
import com.example.core.model.StepStatus
import com.example.data.local.entity.ChatMessageEntity
import com.example.ui.components.StatusOrb
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    aerisCore: AerisCore,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToAuto: () -> Unit,
    onOpenVoiceCall: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val agentStatus by aerisCore.agentStatus.collectAsState()
    val statusMessage by aerisCore.statusMessage.collectAsState()
    val activeSessionId by aerisCore.activeSessionId.collectAsState()
    val activePlan by aerisCore.taskPlanner.currentPlan.collectAsState()

    val messagesFlow = remember(activeSessionId) {
        aerisCore.chatDao.getMessagesForSession(activeSessionId)
    }
    val messages by messagesFlow.collectAsState(initial = emptyList())

    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val listState = rememberLazyListState()

    // Photo picker for screen / UI images (using Play Policy recommended Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AerisDarkBackground)
            .testTag("home_screen_container")
    ) {
        // Status & Agent Mission Banner
        AnimatedVisibility(visible = agentStatus != AgentStatus.IDLE || activePlan != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("agent_mission_card"),
                colors = CardDefaults.cardColors(
                    containerColor = AerisDarkSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusOrb(status = agentStatus, size = 20.dp)
                        Text(
                            text = statusMessage,
                            color = AerisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (activePlan != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mission: ${activePlan?.goal}",
                            color = AerisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Steps Progress
                        activePlan?.steps?.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val icon = when (step.status) {
                                    StepStatus.COMPLETED -> Icons.Default.CheckCircle to AerisEmerald
                                    StepStatus.IN_PROGRESS -> Icons.Default.PlayArrow to AerisCyan
                                    StepStatus.VERIFYING -> Icons.Default.Verified to AerisAmber
                                    StepStatus.FAILED -> Icons.Default.Error to AerisRose
                                    else -> Icons.Default.Circle to AerisTextMuted
                                }
                                Icon(
                                    imageVector = icon.first,
                                    contentDescription = step.status.name,
                                    tint = icon.second,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${step.order}. ${step.title}",
                                    color = if (step.status == StepStatus.COMPLETED) AerisTextSecondary else AerisTextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Conversation Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    EmptyHomeState(
                        onQuickAction = { prompt ->
                            inputText = prompt
                        }
                    )
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    MessageCard(
                        message = msg,
                        onOpenEditor = { onNavigateToEditor(null) },
                        onOpenPreview = onNavigateToPreview
                    )
                }
            }
        }

        // Quick Suggestions Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SuggestionChip(
                    text = "Create Three.js 3D Project",
                    icon = Icons.Default.Code,
                    onClick = {
                        inputText = "Create a Three.js 3D rotating glowing sphere with particles and run preview"
                    }
                )
            }
            item {
                SuggestionChip(
                    text = "Automate Android Task",
                    icon = Icons.Default.ElectricBolt,
                    onClick = {
                        inputText = "Open Android Settings, check accessibility status, and verify connection"
                    }
                )
            }
            item {
                SuggestionChip(
                    text = "Inspect Screen UI",
                    icon = Icons.Default.Visibility,
                    onClick = {
                        inputText = "Read the current screen hierarchy, analyze visible interactive buttons, and report state"
                    }
                )
            }
        }

        // Selected Image Preview Pill
        if (selectedImageUri != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AerisDarkSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Attached Screen Image",
                    tint = AerisCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Screen Image Attached (Qwen 3.8-27B ready)",
                    color = AerisTextSecondary,
                    fontSize = 11.sp
                )
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Image",
                        tint = AerisRose,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Input Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AerisSurface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attach Screenshot / Image for Qwen
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AerisDarkSurfaceVariant)
                        .testTag("attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Screen Image for Qwen Vision",
                        tint = if (selectedImageUri != null) AerisCyan else AerisTextSecondary
                    )
                }

                // Voice Calling Button
                IconButton(
                    onClick = onOpenVoiceCall,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AerisViolet.copy(alpha = 0.2f))
                        .testTag("home_voice_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Mode",
                        tint = AerisViolet
                    )
                }

                // Text Input Field
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Command Aeris or describe goal...",
                            color = AerisTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_input_field"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AerisDarkSurfaceVariant,
                        unfocusedContainerColor = AerisDarkSurfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = AerisTextPrimary,
                        unfocusedTextColor = AerisTextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4
                )

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val prompt = inputText.trim()
                            val imagePath = selectedImageUri?.toString()
                            val imgType = if (imagePath != null) "image" else null
                            inputText = ""
                            selectedImageUri = null

                            aerisCore.processUserInput(
                                input = prompt,
                                attachmentPath = imagePath,
                                attachmentType = imgType
                            )
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) AerisCyan else AerisDarkSurfaceVariant)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Command",
                        tint = if (inputText.isNotBlank()) AerisDarkOnPrimary else AerisTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHomeState(onQuickAction: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusOrb(status = AgentStatus.IDLE, size = 80.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AERIS AGENT WORKSPACE",
            color = AerisCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Multi-Model Intelligence: GPT-OSS 120B • Qwen 3.8-27B • Whisper V3",
            color = AerisTextSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AerisDarkSurfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Core Agent Capabilities",
                    color = AerisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                CapabilityItem("Autonomous Planning", "Decomposes goals into verifiable multi-step execution paths.")
                CapabilityItem("Hybrid Automation", "Accessibility node inspection + official Intents + Shizuku backend.")
                CapabilityItem("AI Code Studio", "Generates web/Three.js apps, runs live testing, and triggers Auto Fix.")
                CapabilityItem("Multi-Profile Browser", "Coordinates web research across isolated session containers.")
            }
        }
    }
}

@Composable
private fun CapabilityItem(title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = AerisCyan,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(text = title, color = AerisTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(text = desc, color = AerisTextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = AerisDarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, AerisDarkOutline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AerisCyan, modifier = Modifier.size(14.dp))
            Text(text = text, color = AerisTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MessageCard(
    message: ChatMessageEntity,
    onOpenEditor: () -> Unit,
    onOpenPreview: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .testTag("message_card_${message.id}"),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) AerisDarkPrimaryContainer.copy(alpha = 0.5f) else AerisDarkSurfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, AerisDarkOutline) else null
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header badge
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AerisViolet.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = message.modelName ?: "GPT-OSS 120B",
                                color = AerisViolet,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (message.verificationEvidence != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AerisEmerald.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = AerisEmerald,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "VERIFIED",
                                        color = AerisEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Message Text Content
                Text(
                    text = message.content,
                    color = AerisTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Tool verification evidence box
                if (message.verificationEvidence != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(AerisObsidian)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Evidence: ${message.verificationEvidence}",
                            color = AerisCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Workspace shortcut actions
                if (!isUser && (message.content.contains("Three.js", ignoreCase = true) || message.content.contains("html", ignoreCase = true) || message.content.contains("code", ignoreCase = true))) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenEditor,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AerisCyan)
                        ) {
                            Text("Open in Editor", color = AerisDarkOnPrimary, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onOpenPreview,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Run Preview", color = AerisCyan, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
