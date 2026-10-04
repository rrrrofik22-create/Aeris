package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.model.AgentStatus
import com.example.ui.theme.*

@Composable
fun StatusOrb(
    status: AgentStatus,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val (coreColor, ringColor) = when (status) {
        AgentStatus.IDLE -> AerisCyan to AerisCyanDark
        AgentStatus.LISTENING -> AerisCyan to AerisViolet
        AgentStatus.THINKING, AgentStatus.PLANNING -> AerisViolet to AerisCyan
        AgentStatus.EXECUTING -> AerisAmber to AerisViolet
        AgentStatus.OBSERVING, AgentStatus.VERIFYING -> AerisEmerald to AerisCyan
        AgentStatus.SPEAKING -> AerisCyan to AerisEmerald
        AgentStatus.SUCCESS -> AerisEmerald to AerisCyan
        AgentStatus.ERROR, AgentStatus.RECOVERING -> AerisRose to AerisAmber
        else -> AerisCyan to AerisViolet
    }

    Canvas(modifier = modifier.size(size)) {
        val center = this.center
        val radius = this.size.minDimension / 2.8f * pulseScale

        // Outer glow ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ringColor.copy(alpha = 0.4f), Color.Transparent),
                center = center,
                radius = radius * 1.5f
            ),
            center = center,
            radius = radius * 1.4f
        )

        // Orbital rotating ring
        drawCircle(
            color = ringColor.copy(alpha = 0.7f),
            center = center,
            radius = radius * 1.1f,
            style = Stroke(width = 2.dp.toPx())
        )

        // Central glowing core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, coreColor),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius * 0.75f
        )
    }
}
