package com.example.ui.calendar

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AshGrey
import kotlinx.coroutines.launch

/**
 * Interactive 3D Calendar Hero widget with real-time perspective tilt,
 * touch gestures, and subtle idle floating physics.
 */
@Composable
fun InteractiveCalendar3dWidget(
    modifier: Modifier = Modifier,
    sizeDp: Int = 62
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }

    val rotY = remember { Animatable(-7f) }
    val rotX = remember { Animatable(9f) }

    // Subtle idle floating animation when not dragging
    val infiniteTransition = rememberInfiniteTransition(label = "idle_cal_float")
    val idleBob by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_cal_bob"
    )
    val idleTurntableDrift by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_cal_drift"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, AshGrey.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        coroutineScope.launch {
                            rotY.animateTo(-7f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 220f))
                        }
                        coroutineScope.launch {
                            rotX.animateTo(9f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 220f))
                        }
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newY = (rotY.value + dragAmount.x * 0.5f).coerceIn(-42f, 42f)
                            val newX = (rotX.value - dragAmount.y * 0.5f).coerceIn(-28f, 28f)
                            rotY.snapTo(newY)
                            rotX.snapTo(newX)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val densityVal = androidx.compose.ui.platform.LocalDensity.current.density
        Box(
            modifier = Modifier
                .size((sizeDp - 8).dp)
                .graphicsLayer {
                    val rotYVal = if (isDragging) rotY.value else rotY.value + idleTurntableDrift
                    val rotXVal = if (isDragging) rotX.value else rotX.value + (idleBob * 0.45f)
                    val transYVal = if (isDragging) 0f else idleBob

                    rotationY = rotYVal
                    rotationX = rotXVal
                    translationY = transYVal
                    cameraDistance = 14f * densityVal
                    shadowElevation = 6.dp.toPx()
                    shape = RoundedCornerShape(14.dp)
                    clip = true
                }
                .drawWithContent {
                    drawContent()
                    val rotYVal = if (isDragging) rotY.value else rotY.value + idleTurntableDrift
                    val rotXVal = if (isDragging) rotX.value else rotX.value + (idleBob * 0.45f)
                    val highlightX = size.width * (0.5f + (rotYVal / 90f))
                    val highlightY = size.height * (0.35f + (rotXVal / 90f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(highlightX, highlightY),
                            radius = size.width * 0.6f
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_3d_calendar_1790178917428),
                contentDescription = "3D Calendar Hero",
                modifier = Modifier.size((sizeDp - 8).dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}
