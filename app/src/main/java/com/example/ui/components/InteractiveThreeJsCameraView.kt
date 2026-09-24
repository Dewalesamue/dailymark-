package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Accent
import com.example.ui.theme.AshGrey
import kotlinx.coroutines.launch

/**
 * High-performance, native 3D interactive camera hero component.
 * Uses Jetpack Compose 3D graphicsLayer perspective projection and touch gesture tracking,
 * completely avoiding Mesa / WebGL hardware rendernode crashes on virtualized Android environments.
 */
@Composable
fun InteractiveThreeJsCameraView(
    modifier: Modifier = Modifier,
    sizeDp: Int = 130,
    showHintBadge: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    val rotY = remember { Animatable(-8f) }
    val rotX = remember { Animatable(10f) }

    // Subtle idle floating animation when not dragging
    val infiniteTransition = rememberInfiniteTransition(label = "idle_floating")
    val idleBob by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_bob"
    )
    val idleTurntableDrift by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_turntable"
    )

    Column(
        modifier = modifier.testTag("interactive_threejs_camera_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(1.dp, AshGrey.copy(alpha = 0.25f), RoundedCornerShape(32.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            // Smooth spring back to pleasant showcase angle
                            coroutineScope.launch {
                                rotY.animateTo(-8f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f))
                            }
                            coroutineScope.launch {
                                rotX.animateTo(10f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f))
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newY = (rotY.value + dragAmount.x * 0.45f).coerceIn(-45f, 45f)
                                val newX = (rotX.value - dragAmount.y * 0.45f).coerceIn(-28f, 28f)
                                rotY.snapTo(newY)
                                rotX.snapTo(newX)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val currentRotY = if (isDragging) rotY.value else rotY.value + idleTurntableDrift
            val currentRotX = if (isDragging) rotX.value else rotX.value + (idleBob * 0.5f)
            val currentTranslationY = if (isDragging) 0f else idleBob

            Box(
                modifier = Modifier
                    .size((sizeDp - 14).dp)
                    .graphicsLayer {
                        rotationY = currentRotY
                        rotationX = currentRotX
                        translationY = currentTranslationY
                        cameraDistance = 16f * density
                        shadowElevation = 8.dp.toPx()
                        shape = RoundedCornerShape(26.dp)
                        clip = true
                    }
                    .drawWithContent {
                        drawContent()
                        // Dynamic 3D lighting reflection that shifts with perspective rotation
                        val highlightX = size.width * (0.5f + (currentRotY / 100f))
                        val highlightY = size.height * (0.35f + (currentRotX / 100f))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                center = Offset(highlightX, highlightY),
                                radius = size.width * 0.65f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_3d_camera_1790178903986),
                    contentDescription = "3D Camera Journal",
                    modifier = Modifier.size((sizeDp - 14).dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
