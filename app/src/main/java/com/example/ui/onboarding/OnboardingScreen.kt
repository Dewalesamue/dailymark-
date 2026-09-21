package com.example.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String
)

private val steps = listOf(
    OnboardingStep(
        title = "One photo,\nevery day.",
        subtitle = "Capture a meaningful moment each day to create a visual journal of your journey.",
        icon = Icons.Default.CameraAlt,
        badge = "DAILY CAPTURE"
    ),
    OnboardingStep(
        title = "Watch your life\nunfold.",
        subtitle = "Your marks form an elegant visual calendar. Revisit any day, month, or season with a single glance.",
        icon = Icons.Default.CalendarMonth,
        badge = "VISUAL CALENDAR"
    ),
    OnboardingStep(
        title = "Your Personal\nVisual Journal.",
        subtitle = "Sign in with your own Gmail or email to keep your memories organized and backed up. Simple, private, and effortless.",
        icon = Icons.Default.NotificationsActive,
        badge = "DEMO AUTH"
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Progress Indicator Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                steps.indices.forEach { index ->
                    val isCompleted = index <= currentStepIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isCompleted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            // Main Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val current = steps[currentStepIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = current.badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = current.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(60.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = current.title,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = current.subtitle,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Bottom Navigation: "Skip >>" on left, Deep Charcoal Pill Button "Next" on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Button
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text(
                        text = "Skip >>",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Next / Get Started Button (Deep Charcoal pill button from image)
                Button(
                    onClick = {
                        if (currentStepIndex < steps.size - 1) {
                            currentStepIndex++
                        } else {
                            onComplete()
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .width(130.dp)
                        .testTag(if (currentStepIndex < steps.size - 1) "onboarding_next_button" else "get_started_button")
                ) {
                    Text(
                        text = if (currentStepIndex < steps.size - 1) "Next" else "Get Started",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Custom Canvas drawing of the cute smiling face with big dark expressive anime eyes
 * and blush from the user's reference mockup (Screen 1).
 */
@Composable
private fun CuteFaceIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val eyeDistance = size.width * 0.22f
        val eyeRadius = size.width * 0.12f
        val eyeY = centerY - size.height * 0.05f

        // Left Eye (big dark circle with highlight)
        drawCircle(
            color = Color(0xFF1E1C1A),
            radius = eyeRadius,
            center = Offset(centerX - eyeDistance, eyeY)
        )
        // Left Eye highlight
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.38f,
            center = Offset(centerX - eyeDistance + eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f)
        )

        // Right Eye (big dark circle with highlight)
        drawCircle(
            color = Color(0xFF1E1C1A),
            radius = eyeRadius,
            center = Offset(centerX + eyeDistance, eyeY)
        )
        // Right Eye highlight
        drawCircle(
            color = Color.White,
            radius = eyeRadius * 0.38f,
            center = Offset(centerX + eyeDistance + eyeRadius * 0.3f, eyeY - eyeRadius * 0.3f)
        )

        // Rosy Cheeks
        drawCircle(
            color = Color(0xFFE57373).copy(alpha = 0.45f),
            radius = eyeRadius * 0.55f,
            center = Offset(centerX - eyeDistance - eyeRadius * 0.6f, eyeY + eyeRadius * 0.85f)
        )
        drawCircle(
            color = Color(0xFFE57373).copy(alpha = 0.45f),
            radius = eyeRadius * 0.55f,
            center = Offset(centerX + eyeDistance + eyeRadius * 0.6f, eyeY + eyeRadius * 0.85f)
        )

        // Cute smiling mouth arc
        val mouthY = centerY + size.height * 0.2f
        val mouthWidth = size.width * 0.18f
        drawLine(
            color = Color(0xFF1E1C1A),
            start = Offset(centerX - mouthWidth, mouthY),
            end = Offset(centerX + mouthWidth, mouthY),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

