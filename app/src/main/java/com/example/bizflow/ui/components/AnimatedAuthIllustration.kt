package com.example.bizflow.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class AuthFocusState {
    IDLE,
    EMAIL,
    PASSWORD,
    AUTHENTICATED
}

@Composable
fun AnimatedAuthIllustration(
    focusState: AuthFocusState = AuthFocusState.IDLE,
    isSuccess: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Breathing floating motion
    val infiniteTransition = rememberInfiniteTransition(label = "CharacterFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatingAnimation"
    )

    // Periodic blinking state
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3200)
            isBlinking = true
            delay(160)
            isBlinking = false
        }
    }

    // Pupil shift targets based on focused field
    val (targetPupilX, targetPupilY) = when (focusState) {
        AuthFocusState.EMAIL -> Pair(3f, 4f)
        AuthFocusState.PASSWORD -> Pair(0f, 6f)
        AuthFocusState.AUTHENTICATED -> Pair(0f, -2f)
        AuthFocusState.IDLE -> Pair(0f, 0f)
    }

    val pupilOffsetX by animateFloatAsState(targetValue = targetPupilX, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "PupilX")
    val pupilOffsetY by animateFloatAsState(targetValue = targetPupilY, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "PupilY")

    val isDark = MaterialTheme.colorScheme.surface == SurfaceDark || MaterialTheme.colorScheme.background == SurfaceDark
    val illustrationBg = if (isDark) Color(0xFF131823) else MivornexaPurpleSoft
    val shadowColor = if (isDark) Color(0xFF090C12).copy(alpha = 0.85f) else Color(0xFFDDD6FE).copy(alpha = 0.7f)
    val pebbleColor = if (isDark) Color(0xFF2C3549) else Color(0xFF1E232E)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(illustrationBg)
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            val width = size.width
            val height = size.height
            val baseY = height * 0.85f

            // 1. Shadow beneath characters
            drawOval(
                color = shadowColor,
                topLeft = Offset(width * 0.1f, baseY - 6f),
                size = Size(width * 0.8f, 22f)
            )

            // 2. Character A: Pebble (Left-Center)
            drawRoundRect(
                color = pebbleColor,
                topLeft = Offset(width * 0.18f, baseY - 90f + floatOffset * 0.5f),
                size = Size(width * 0.22f, 90f),
                cornerRadius = CornerRadius(36f, 36f)
            )
            // Left character eyes
            drawCharacterEyes(
                leftEyePos = Offset(width * 0.24f, baseY - 62f + floatOffset * 0.5f),
                rightEyePos = Offset(width * 0.32f, baseY - 62f + floatOffset * 0.5f),
                eyeRadius = 8f,
                pupilOffsetX = pupilOffsetX * 0.8f,
                pupilOffsetY = pupilOffsetY * 0.8f,
                isBlinking = isBlinking,
                isJoyful = isSuccess
            )

            // 3. Character B: Tall Vivid Purple Pillar with Arched Head (Center)
            val purpleLeft = width * 0.38f
            val purpleWidth = width * 0.26f
            val purpleTop = baseY - 170f + floatOffset
            drawRoundRect(
                color = MivornexaPurple,
                topLeft = Offset(purpleLeft, purpleTop),
                size = Size(purpleWidth, 170f),
                cornerRadius = CornerRadius(44f, 44f)
            )
            // Subtle purple highlight
            drawCircle(
                color = MivornexaPurpleLight.copy(alpha = 0.35f),
                radius = 18f,
                center = Offset(purpleLeft + purpleWidth * 0.75f, purpleTop + 24f)
            )
            // Center purple character eyes
            drawCharacterEyes(
                leftEyePos = Offset(purpleLeft + purpleWidth * 0.33f, purpleTop + 48f),
                rightEyePos = Offset(purpleLeft + purpleWidth * 0.67f, purpleTop + 48f),
                eyeRadius = 11f,
                pupilOffsetX = pupilOffsetX,
                pupilOffsetY = pupilOffsetY,
                isBlinking = isBlinking,
                isJoyful = isSuccess
            )

            // 4. Character C: Warm Orange Pill Character (Right-Center)
            val orangeLeft = width * 0.62f
            val orangeWidth = width * 0.24f
            val orangeTop = baseY - 125f - floatOffset * 0.7f
            drawRoundRect(
                color = MivornexaOrange,
                topLeft = Offset(orangeLeft, orangeTop),
                size = Size(orangeWidth, 125f),
                cornerRadius = CornerRadius(40f, 40f)
            )
            // Orange character eyes
            drawCharacterEyes(
                leftEyePos = Offset(orangeLeft + orangeWidth * 0.32f, orangeTop + 40f),
                rightEyePos = Offset(orangeLeft + orangeWidth * 0.68f, orangeTop + 40f),
                eyeRadius = 9f,
                pupilOffsetX = pupilOffsetX * 1.2f,
                pupilOffsetY = pupilOffsetY * 1.2f,
                isBlinking = isBlinking,
                isJoyful = isSuccess
            )

            // 5. Character D: Tiny Playful Sunny Yellow Dome (Front Right)
            val yellowCenter = Offset(width * 0.58f, baseY - 24f + floatOffset * 0.3f)
            drawArc(
                color = MivornexaYellow,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(yellowCenter.x - 28f, yellowCenter.y - 28f),
                size = Size(56f, 56f)
            )
            // Yellow character tiny dot eyes
            if (!isBlinking) {
                drawCircle(color = Color(0xFF1E232E), radius = 2.5f, center = Offset(yellowCenter.x - 8f, yellowCenter.y - 12f))
                drawCircle(color = Color(0xFF1E232E), radius = 2.5f, center = Offset(yellowCenter.x + 8f, yellowCenter.y - 12f))
            } else {
                drawLine(
                    color = Color(0xFF1E232E),
                    start = Offset(yellowCenter.x - 10f, yellowCenter.y - 12f),
                    end = Offset(yellowCenter.x - 6f, yellowCenter.y - 12f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0xFF1E232E),
                    start = Offset(yellowCenter.x + 6f, yellowCenter.y - 12f),
                    end = Offset(yellowCenter.x + 10f, yellowCenter.y - 12f),
                    strokeWidth = 2f
                )
            }
        }
    }
}

private fun DrawScope.drawCharacterEyes(
    leftEyePos: Offset,
    rightEyePos: Offset,
    eyeRadius: Float,
    pupilOffsetX: Float,
    pupilOffsetY: Float,
    isBlinking: Boolean,
    isJoyful: Boolean
) {
    if (isJoyful) {
        // Joyful smiling arcs ^ ^
        val pathLeft = Path().apply {
            moveTo(leftEyePos.x - eyeRadius, leftEyePos.y + 2f)
            quadraticBezierTo(leftEyePos.x, leftEyePos.y - eyeRadius * 0.9f, leftEyePos.x + eyeRadius, leftEyePos.y + 2f)
        }
        val pathRight = Path().apply {
            moveTo(rightEyePos.x - eyeRadius, rightEyePos.y + 2f)
            quadraticBezierTo(rightEyePos.x, rightEyePos.y - eyeRadius * 0.9f, rightEyePos.x + eyeRadius, rightEyePos.y + 2f)
        }
        drawPath(path = pathLeft, color = Color(0xFF0F172A), style = Stroke(width = 3.5f))
        drawPath(path = pathRight, color = Color(0xFF0F172A), style = Stroke(width = 3.5f))
    } else if (isBlinking) {
        // Closed horizontal eyelid lines during blink
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(leftEyePos.x - eyeRadius * 0.9f, leftEyePos.y),
            end = Offset(leftEyePos.x + eyeRadius * 0.9f, leftEyePos.y),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(rightEyePos.x - eyeRadius * 0.9f, rightEyePos.y),
            end = Offset(rightEyePos.x + eyeRadius * 0.9f, rightEyePos.y),
            strokeWidth = 3f
        )
    } else {
        // Open wide white eye sclera
        drawCircle(color = Color.White, radius = eyeRadius, center = leftEyePos)
        drawCircle(color = Color.White, radius = eyeRadius, center = rightEyePos)

        // Dark pupil with gentle offset tracking
        val pupilRadius = eyeRadius * 0.52f
        val pupilCenterLeft = Offset(leftEyePos.x + pupilOffsetX.coerceIn(-eyeRadius * 0.35f, eyeRadius * 0.35f), leftEyePos.y + pupilOffsetY.coerceIn(-eyeRadius * 0.35f, eyeRadius * 0.35f))
        val pupilCenterRight = Offset(rightEyePos.x + pupilOffsetX.coerceIn(-eyeRadius * 0.35f, eyeRadius * 0.35f), rightEyePos.y + pupilOffsetY.coerceIn(-eyeRadius * 0.35f, eyeRadius * 0.35f))

        drawCircle(color = Color(0xFF0F172A), radius = pupilRadius, center = pupilCenterLeft)
        drawCircle(color = Color(0xFF0F172A), radius = pupilRadius, center = pupilCenterRight)

        // Tiny white light sparkle reflection in eye
        drawCircle(color = Color.White, radius = pupilRadius * 0.35f, center = Offset(pupilCenterLeft.x - pupilRadius * 0.3f, pupilCenterLeft.y - pupilRadius * 0.3f))
        drawCircle(color = Color.White, radius = pupilRadius * 0.35f, center = Offset(pupilCenterRight.x - pupilRadius * 0.3f, pupilCenterRight.y - pupilRadius * 0.3f))
    }
}
