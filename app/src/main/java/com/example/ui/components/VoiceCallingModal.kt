package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.model.AgentStatus
import com.example.core.voice.VoiceManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun VoiceCallingModal(
    voiceManager: VoiceManager,
    onDismiss: () -> Unit,
    onTranscriptionReady: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val voiceState by voiceManager.voiceState.collectAsState()
    val transcript by voiceManager.transcript.collectAsState()
    var isMuted by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            voiceManager.stopSpeaking()
            voiceManager.cancelListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AerisObsidian.copy(alpha = 0.98f),
                            Color(0xFF0F172A),
                            AerisObsidian
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AERIS VOICE AGENT",
                        color = AerisCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Whisper V3 Turbo • GPT-OSS 120B • Neural Speech",
                        color = AerisTextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Pulsing Center Orb
                Box(contentAlignment = Alignment.Center) {
                    StatusOrb(status = voiceState, size = 160.dp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Status Indicator
                Text(
                    text = when (voiceState) {
                        AgentStatus.LISTENING -> "Listening to your voice..."
                        AgentStatus.THINKING -> "Processing audio via Whisper Large V3..."
                        AgentStatus.SPEAKING -> "Aeris Speaking..."
                        else -> "Tap microphone to speak"
                    },
                    color = when (voiceState) {
                        AgentStatus.LISTENING -> AerisCyan
                        AgentStatus.SPEAKING -> AerisEmerald
                        AgentStatus.THINKING -> AerisViolet
                        else -> AerisTextSecondary
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Transcription Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp, max = 120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AerisDarkSurfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, AerisDarkOutline, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = transcript ?: "Aeris is ready for natural voice interaction. Hands-free calling mode active.",
                        color = AerisTextPrimary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Voice Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Barge-in / Stop speaking button
                    IconButton(
                        onClick = {
                            voiceManager.stopSpeaking()
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(AerisDarkSurfaceVariant)
                            .border(1.dp, AerisDarkOutline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Barge-in / Stop",
                            tint = AerisAmber
                        )
                    }

                    // Push to Talk / Record Toggle
                    IconButton(
                        onClick = {
                            if (voiceState == AgentStatus.LISTENING) {
                                coroutineScope.launch {
                                    val text = voiceManager.stopListeningAndTranscribe()
                                    if (!text.isNullOrBlank()) {
                                        onTranscriptionReady(text)
                                    }
                                }
                            } else {
                                voiceManager.startListening()
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (voiceState == AgentStatus.LISTENING) AerisRose else AerisCyan
                            )
                            .testTag("voice_talk_button")
                    ) {
                        Icon(
                            imageVector = if (voiceState == AgentStatus.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Toggle Mic",
                            tint = AerisDarkOnPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // End Call
                    IconButton(
                        onClick = {
                            voiceManager.stopSpeaking()
                            voiceManager.cancelListening()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(AerisRose.copy(alpha = 0.2f))
                            .border(1.dp, AerisRose.copy(alpha = 0.6f), CircleShape)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = AerisRose
                        )
                    }
                }
            }
        }
    }
}
