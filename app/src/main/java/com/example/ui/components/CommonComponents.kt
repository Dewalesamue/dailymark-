package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AshGrey
import com.example.ui.theme.Charcoal
import com.example.ui.theme.Porcelain
import com.example.ui.theme.SandyClay
import com.example.ui.theme.SunlitClay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val AvailableMoods = listOf(
    "Joyful" to "☀️",
    "Peaceful" to "🌿",
    "Cozy" to "☕",
    "Inspired" to "💫",
    "Reflective" to "🌧️",
    "Grateful" to "💛",
    "Celebratory" to "🎉",
    "Adventurous" to "🏔️"
)

fun getMoodEmoji(moodName: String?): String {
    if (moodName == null) return ""
    return AvailableMoods.find { it.first.equals(moodName, ignoreCase = true) }?.second ?: "✨"
}

fun formatFriendlyDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        when (date) {
            today -> "Today, ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
            yesterday -> "Yesterday, ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
        }
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
fun MoodChip(
    moodName: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emoji = getMoodEmoji(moodName)

    val bgColor = if (isSelected) {
        SunlitClay
    } else {
        AshGrey.copy(alpha = 0.2f)
    }

    val contentColor = Charcoal

    val borderColor = if (isSelected) {
        Charcoal
    } else {
        AshGrey.copy(alpha = 0.4f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect)
            .testTag("mood_chip_$moodName")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = moodName,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun MoodSelectorRow(
    selectedMood: String?,
    onMoodSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(AvailableMoods) { (name, _) ->
            val isSelected = selectedMood.equals(name, ignoreCase = true)
            MoodChip(
                moodName = name,
                isSelected = isSelected,
                onSelect = {
                    if (isSelected) onMoodSelected(null) else onMoodSelected(name)
                }
            )
        }
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Charcoal,
            contentColor = Porcelain
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
