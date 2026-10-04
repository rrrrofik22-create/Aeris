package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AgentStatus
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AerisTopBar(
    currentTitle: String,
    status: AgentStatus,
    activeModel: String,
    onVoiceCallClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusOrb(status = status, size = 32.dp)
                Column {
                    Text(
                        text = currentTitle,
                        color = AerisTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AerisDarkPrimaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GPT-OSS 120B",
                                color = AerisCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "• ${status.name}",
                            color = when (status) {
                                AgentStatus.IDLE -> AerisTextMuted
                                AgentStatus.ERROR, AgentStatus.RECOVERING -> AerisRose
                                AgentStatus.VERIFYING -> AerisEmerald
                                else -> AerisCyan
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        actions = {
            // Voice Calling Mode Button
            IconButton(
                onClick = onVoiceCallClick,
                modifier = Modifier
                    .testTag("voice_call_button")
                    .padding(end = 4.dp)
                    .clip(CircleShape)
                    .background(AerisViolet.copy(alpha = 0.15f))
                    .border(1.dp, AerisViolet.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Open Voice Call Mode",
                    tint = AerisViolet
                )
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .testTag("settings_button")
                    .padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Open Settings",
                    tint = AerisTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AerisObsidian
        )
    )
}
