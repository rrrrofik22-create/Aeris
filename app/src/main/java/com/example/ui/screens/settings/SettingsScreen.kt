package com.example.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.core.ai.ApiKeyManager
import com.example.core.ai.ModelRouter
import com.example.core.automation.AerisAccessibilityService
import com.example.core.automation.ShizukuStatus
import com.example.data.local.entity.ApiKeyEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.ui.theme.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    aerisCore: AerisCore,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val modelConfig by aerisCore.modelRouter.config.collectAsState()

    val apiKeysFlow: Flow<List<ApiKeyEntity>> = remember { aerisCore.apiKeyManager.allKeys }
    val apiKeys: List<ApiKeyEntity> by apiKeysFlow.collectAsState(initial = emptyList())

    val memoriesFlow: Flow<List<MemoryItemEntity>> = remember { aerisCore.database.memoryDao().getAllMemories() }
    val memories: List<MemoryItemEntity> by memoriesFlow.collectAsState(initial = emptyList())

    val isAccessibilityActive by AerisAccessibilityService.isConnected.collectAsState()
    val shizukuStatus by aerisCore.shizukuEngine.status.collectAsState()

    var showAddKeyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Aeris Agent Settings",
                        color = AerisTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AerisCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AerisObsidian)
            )
        },
        containerColor = AerisDarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Section 1: AI Model Router
            item {
                SectionHeader("AI MODEL ROUTER", "Centralized, configurable model orchestration")
                Card(
                    colors = CardDefaults.cardColors(containerColor = AerisDarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModelSettingItem(
                            label = "Main Reasoning Brain",
                            modelName = modelConfig.mainModel,
                            provider = "Groq",
                            description = "Natural language understanding, planning, tool execution, code generation"
                        )
                        HorizontalDivider(color = AerisDarkOutline)
                        ModelSettingItem(
                            label = "Vision Specialist",
                            modelName = modelConfig.visionModel,
                            provider = "Groq",
                            description = "Screen understanding, UI element detection, image reasoning"
                        )
                        HorizontalDivider(color = AerisDarkOutline)
                        ModelSettingItem(
                            label = "Speech-To-Text (STT)",
                            modelName = modelConfig.sttModel,
                            provider = "Groq",
                            description = "High-speed voice transcription pipeline"
                        )
                    }
                }
            }

            // Section 2: Multiple API Key Management & Failover
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("GROQ API KEYS & FAILOVER", "Automatic cooldown and key rotation")
                    Button(
                        onClick = { showAddKeyDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AerisCyan),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("add_api_key_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AerisDarkOnPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Key", color = AerisDarkOnPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = AerisDarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (apiKeys.isEmpty()) {
                            Text(
                                text = "No API keys added yet. Add one or more Groq keys (gsk_...) to enable live AI reasoning, Qwen Vision, and Whisper STT.",
                                color = AerisTextSecondary,
                                fontSize = 11.sp
                            )
                        } else {
                            apiKeys.forEach { key: ApiKeyEntity ->
                                ApiKeyRow(
                                    keyEntity = key,
                                    onDelete = {
                                        coroutineScope.launch {
                                            aerisCore.apiKeyManager.deleteKey(key.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Automation & Permissions
            item {
                SectionHeader("AUTOMATION & DEVICE PERMISSIONS", "Accessibility + Shizuku hybrid architecture")
                Card(
                    colors = CardDefaults.cardColors(containerColor = AerisDarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Accessibility Service", color = AerisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (isAccessibilityActive) "Active • Capturing UI nodes" else "Inactive • Required for automated interaction",
                                    color = if (isAccessibilityActive) AerisEmerald else AerisRose,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = { aerisCore.intentEngine.openSystemSettings("accessibility") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isAccessibilityActive) AerisDarkSurfaceVariant else AerisCyan),
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(if (isAccessibilityActive) "Config" else "Enable", fontSize = 10.sp)
                            }
                        }

                        HorizontalDivider(color = AerisDarkOutline)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Shizuku System Backend", color = AerisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = when (shizukuStatus) {
                                        ShizukuStatus.CONNECTED_AND_ACTIVE -> "Active (System Binder Ready)"
                                        ShizukuStatus.NOT_INSTALLED -> "Not installed (Optional backend)"
                                        else -> "Installed (Service not running)"
                                    },
                                    color = AerisTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { aerisCore.shizukuEngine.refreshStatus() }, modifier = Modifier.size(30.dp)) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Shizuku", tint = AerisCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Section 4: Memory Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("AERIS 8-LAYER MEMORY", "${memories.size} facts remembered")
                    if (memories.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    aerisCore.contextManager.clearAllMemory()
                                }
                            },
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Clear All", color = AerisRose, fontSize = 10.sp)
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = AerisDarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Long-Term Memory Active", color = AerisTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Switch(
                                checked = modelConfig.isMemoryEnabled,
                                onCheckedChange = { aerisCore.modelRouter.setMemoryEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AerisCyan)
                            )
                        }

                        if (memories.isEmpty()) {
                            Text("No memories recorded yet. Aeris learns user preferences during conversation.", color = AerisTextMuted, fontSize = 11.sp)
                        } else {
                            memories.take(4).forEach { mem: MemoryItemEntity ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mem.summary, color = AerisCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Text(mem.detail, color = AerisTextSecondary, fontSize = 10.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                aerisCore.contextManager.forgetMemory(mem.id)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Forget", tint = AerisRose, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddKeyDialog) {
        var keyLabel by remember { mutableStateOf("") }
        var rawKey by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddKeyDialog = false },
            title = { Text("Add Groq API Key", color = AerisTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = keyLabel,
                        onValueChange = { keyLabel = it },
                        label = { Text("Key Label") },
                        placeholder = { Text("e.g. Groq Primary or Backup") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rawKey,
                        onValueChange = { rawKey = it },
                        label = { Text("API Key (gsk_...)") },
                        placeholder = { Text("gsk_...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("API keys are stored encrypted locally and NEVER exposed in logs.", color = AerisTextMuted, fontSize = 10.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawKey.isNotBlank()) {
                            coroutineScope.launch {
                                aerisCore.apiKeyManager.addKey(keyLabel, rawKey)
                                showAddKeyDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AerisCyan)
                ) {
                    Text("Save Key", color = AerisDarkOnPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddKeyDialog = false }) {
                    Text("Cancel", color = AerisTextMuted)
                }
            },
            containerColor = AerisDarkSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Text(text = title, color = AerisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(text = subtitle, color = AerisTextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun ModelSettingItem(label: String, modelName: String, provider: String, description: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = AerisTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AerisDarkPrimaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = modelName, color = AerisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = description, color = AerisTextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun ApiKeyRow(keyEntity: ApiKeyEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = keyEntity.label, color = AerisTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when (keyEntity.status) {
                                "ACTIVE" -> AerisEmerald.copy(alpha = 0.2f)
                                "COOLDOWN" -> AerisAmber.copy(alpha = 0.2f)
                                else -> AerisRose.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = keyEntity.status,
                        color = when (keyEntity.status) {
                            "ACTIVE" -> AerisEmerald
                            "COOLDOWN" -> AerisAmber
                            else -> AerisRose
                        },
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = ApiKeyManager.maskKey(keyEntity.apiKey),
                color = AerisTextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete Key", tint = AerisRose, modifier = Modifier.size(16.dp))
        }
    }
}
