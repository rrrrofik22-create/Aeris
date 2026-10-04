package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.ui.components.AerisBottomBar
import com.example.ui.components.AerisTopBar
import com.example.ui.components.VoiceCallingModal
import com.example.ui.navigation.AerisWorkspace
import com.example.ui.screens.auto.AutoScreen
import com.example.ui.screens.browser.BrowserScreen
import com.example.ui.screens.editor.EditorScreen
import com.example.ui.screens.files.FilesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.preview.PreviewScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.AerisTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AerisApplication
        val aerisCore = app.aerisCore

        setContent {
            AerisTheme {
                var currentWorkspace by remember { mutableStateOf(AerisWorkspace.HOME) }
                var showSettings by remember { mutableStateOf(false) }
                var showVoiceCalling by remember { mutableStateOf(false) }

                val agentStatus by aerisCore.agentStatus.collectAsState()
                val modelConfig by aerisCore.modelRouter.config.collectAsState()
                val context = LocalContext.current

                // Permission launcher for microphone
                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        showVoiceCalling = true
                    }
                }

                fun requestVoiceMode() {
                    val hasMic = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasMic) {
                        showVoiceCalling = true
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                // Handle back press
                BackHandler(enabled = showSettings || currentWorkspace != AerisWorkspace.HOME) {
                    if (showSettings) {
                        showSettings = false
                    } else if (currentWorkspace != AerisWorkspace.HOME) {
                        currentWorkspace = AerisWorkspace.HOME
                    }
                }

                if (showSettings) {
                    SettingsScreen(
                        aerisCore = aerisCore,
                        onBack = { showSettings = false }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            AerisTopBar(
                                currentTitle = when (currentWorkspace) {
                                    AerisWorkspace.HOME -> "Aeris Agent"
                                    AerisWorkspace.AUTO -> "Auto Workspace"
                                    AerisWorkspace.EDITOR -> "Code Editor"
                                    AerisWorkspace.PREVIEW -> "Live Preview"
                                    AerisWorkspace.BROWSER -> "AI Browser"
                                    AerisWorkspace.FILES -> "Files & Projects"
                                },
                                status = agentStatus,
                                activeModel = modelConfig.mainModel,
                                onVoiceCallClick = { requestVoiceMode() },
                                onSettingsClick = { showSettings = true }
                            )
                        },
                        bottomBar = {
                            AerisBottomBar(
                                currentWorkspace = currentWorkspace,
                                onWorkspaceSelected = { currentWorkspace = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            AnimatedContent(
                                targetState = currentWorkspace,
                                label = "workspace_transition"
                            ) { workspace ->
                                when (workspace) {
                                    AerisWorkspace.HOME -> HomeScreen(
                                        aerisCore = aerisCore,
                                        onNavigateToEditor = { currentWorkspace = AerisWorkspace.EDITOR },
                                        onNavigateToPreview = { currentWorkspace = AerisWorkspace.PREVIEW },
                                        onNavigateToAuto = { currentWorkspace = AerisWorkspace.AUTO },
                                        onOpenVoiceCall = { requestVoiceMode() }
                                    )
                                    AerisWorkspace.AUTO -> AutoScreen(
                                        aerisCore = aerisCore,
                                        onNavigateToSettings = { showSettings = true }
                                    )
                                    AerisWorkspace.EDITOR -> EditorScreen(
                                        aerisCore = aerisCore,
                                        onNavigateToPreview = { currentWorkspace = AerisWorkspace.PREVIEW }
                                    )
                                    AerisWorkspace.PREVIEW -> PreviewScreen(
                                        aerisCore = aerisCore,
                                        onNavigateToEditor = { currentWorkspace = AerisWorkspace.EDITOR }
                                    )
                                    AerisWorkspace.BROWSER -> BrowserScreen(
                                        aerisCore = aerisCore
                                    )
                                    AerisWorkspace.FILES -> FilesScreen(
                                        aerisCore = aerisCore,
                                        onOpenInEditor = { currentWorkspace = AerisWorkspace.EDITOR },
                                        onOpenInPreview = { currentWorkspace = AerisWorkspace.PREVIEW }
                                    )
                                }
                            }
                        }
                    }
                }

                // Hands-Free Calling Mode Modal
                if (showVoiceCalling) {
                    VoiceCallingModal(
                        voiceManager = aerisCore.voiceManager,
                        onDismiss = { showVoiceCalling = false },
                        onTranscriptionReady = { transcript ->
                            showVoiceCalling = false
                            aerisCore.processUserInput(transcript, isVoiceInput = true)
                        }
                    )
                }
            }
        }
    }
}
