package com.example.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.text.font.FontStyle
import com.example.data.model.DailyPhoto
import com.example.ui.JournalUiState
import com.example.ui.components.getMoodEmoji
import com.example.ui.theme.AshGrey
import com.example.ui.theme.Charcoal
import com.example.ui.theme.Porcelain
import com.example.ui.theme.SandyClay
import com.example.ui.theme.SunlitClay
import com.example.util.DateTimeUtils
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    uiState: JournalUiState,
    userName: String,
    profilePictureUri: String? = null,
    onOpenCamera: () -> Unit,
    onSelectGalleryUris: (List<Uri>) -> Unit,
    onOpenPhotoDetail: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenProfile: () -> Unit = onOpenSettings,
    onNavigateToCalendar: () -> Unit,
    onNavigateToMemories: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayPhotos = uiState.todayPhotos
    val todayCount = todayPhotos.size

    val multiGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onSelectGalleryUris(uris)
        }
    }

    val today = remember(uiState.configuredTimeZone) {
        DateTimeUtils.getToday(uiState.configuredTimeZone)
    }

    val todayIso = remember(today) {
        DateTimeUtils.toIsoDate(today)
    }

    val streakDays = remember(uiState.photos, todayIso) {
        var streak = 0
        var checkDate = today
        val datesWithPhotos = uiState.photos.map { it.journalDate }.toSet()
        if (!datesWithPhotos.contains(todayIso)) {
            checkDate = checkDate.minusDays(1)
        }
        while (datesWithPhotos.contains(DateTimeUtils.toIsoDate(checkDate))) {
            streak++
            checkDate = checkDate.minusDays(1)
        }
        streak
    }

    val customTodayTitle = uiState.dayCustomTitles[todayIso]

    var selectedCategoryChip by remember { mutableStateOf("• Today's Moments") }
    val categories = listOf("• Today's Moments", "Memories", "Calendar")

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Top Header: "Hello, Sara!" + Bell (Charcoal & Sandy Clay) & Profile Avatar (Ash Grey)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, $userName!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Circular Charcoal bell button with Porcelain icon & Sandy Clay notification dot
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Charcoal)
                            .clickable(onClick = onOpenNotifications)
                            .testTag("home_notification_bell")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Porcelain,
                            modifier = Modifier.size(20.dp)
                        )
                        // Sandy Clay accent dot
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 2.dp)
                                .clip(CircleShape)
                                .background(SandyClay)
                        )
                    }

                    // Profile avatar circle with Ash Grey ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, AshGrey, CircleShape)
                            .clickable(onClick = onOpenProfile)
                            .testTag("home_profile_button")
                    ) {
                        if (!profilePictureUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = profilePictureUri,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SandyClay)
                            ) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Charcoal
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Category Chips Row: Each chip uniquely styled with palette colors
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategoryChip == cat
                    val (chipBg, chipBorder, chipText) = when (cat) {
                        "• Today's Moments" -> Triple(
                            if (isSelected) SunlitClay else SunlitClay.copy(alpha = 0.2f),
                            SunlitClay,
                            Charcoal
                        )
                        "Memories" -> Triple(
                            if (isSelected) AshGrey else AshGrey.copy(alpha = 0.25f),
                            AshGrey,
                            Charcoal
                        )
                        else -> Triple(
                            if (isSelected) SandyClay else SandyClay.copy(alpha = 0.25f),
                            SandyClay,
                            Charcoal
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = chipBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                selectedCategoryChip = cat
                                when (cat) {
                                    "Memories" -> onNavigateToMemories()
                                    "Calendar" -> onNavigateToCalendar()
                                }
                            }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = chipText,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 3. Featured Hero Card: Today's Mark (Sunlit Clay #E2B56A with Charcoal buttons & Sandy Clay/Ash Grey badges)
        item {
            TerracottaHeroCard(
                todayPhotos = todayPhotos,
                customTodayTitle = customTodayTitle,
                onTakePhoto = onOpenCamera,
                onPickFromGallery = {
                    multiGalleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onOpenPhotoDetail = { id -> onOpenPhotoDetail(id) }
            )
        }

        // 4. Ash Grey Daily Mindset & Stats Card (#A4B5A6 with Charcoal text & Sandy Clay/Sunlit Clay pills)
        item {
            DailyMindsetCard(
                streakDays = streakDays,
                totalMoments = uiState.photos.size,
                onNavigateToMemories = onNavigateToMemories
            )
        }

        // 5. Recent Moments Grid / Carousel
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Moments",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (uiState.photos.isNotEmpty()) {
                        Text(
                            text = "${uiState.photos.size} moments",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.photos.isEmpty()) {
                    EmptyMemoriesState(
                        onTakePhoto = onOpenCamera,
                        onNavigateToCalendar = onNavigateToCalendar
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.photos.take(15)) { photo ->
                            RecentPhotoThumbnailCard(
                                photo = photo,
                                onClick = { onOpenPhotoDetail(photo.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Terracotta Hero Card for Today's Photo Capture.
 */
@Composable
private fun TerracottaHeroCard(
    todayPhotos: List<DailyPhoto>,
    customTodayTitle: String?,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenPhotoDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasPhotos = todayPhotos.isNotEmpty()
    val displayTitle = customTodayTitle ?: if (hasPhotos) "Today's Mark" else "Capture Today"

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SunlitClay),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_prompt_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Row: Camera icon in circle + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Porcelain)
                    ) {
                        Icon(
                            imageVector = if (hasPhotos) Icons.Default.Collections else Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = Charcoal,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = displayTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal
                        )
                        Text(
                            text = if (hasPhotos) "Tap any moment to view full size" else "Keep a mark of every day",
                            fontSize = 12.sp,
                            color = Charcoal.copy(alpha = 0.82f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = if (hasPhotos) {
                    "${todayPhotos.size} moment${if (todayPhotos.size > 1) "s" else ""} captured today • ${todayPhotos.first().caption ?: "A story worth remembering."}"
                } else {
                    "Capture a photo to remember today. Your moments are stored securely on your device."
                },
                fontSize = 13.sp,
                color = Charcoal.copy(alpha = 0.88f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row: Sandy Clay & Ash Grey pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SandyClay,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Charcoal.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = "Today",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = AshGrey,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Charcoal.copy(alpha = 0.18f))
                ) {
                    Text(
                        text = if (todayPhotos.size == 1) "1 Moment" else "${todayPhotos.size} Moments",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom section: Photo thumbnails / Avatars + Action buttons
            if (hasPhotos) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Overlapping preview avatars
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        todayPhotos.take(3).forEachIndexed { index, photo ->
                            Box(
                                modifier = Modifier
                                    .padding(start = if (index > 0) 0.dp else 0.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Porcelain, CircleShape)
                                    .clickable { onOpenPhotoDetail(photo.id) }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photo.displayMediaModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (index < todayPhotos.take(3).size - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }

                        if (todayPhotos.size > 3) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+${todayPhotos.size - 3} more",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Charcoal
                            )
                        }
                    }

                    // Action Button: "+ Add" (Charcoal pill)
                    Button(
                        onClick = onTakePhoto,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Charcoal,
                            contentColor = Porcelain
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("add_another_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Action Buttons for when today has 0 photos: "Take a photo" (Charcoal) and "Gallery" (Sandy Clay)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onTakePhoto,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Charcoal,
                            contentColor = Porcelain
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("take_today_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Take Photo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onPickFromGallery,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SandyClay,
                            contentColor = Charcoal
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("add_from_gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gallery",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ash Grey Mindset & Reflection Card (#A4B5A6).
 * Visually showcases Ash Grey, Charcoal, Sandy Clay, Sunlit Clay, and Porcelain.
 */
@Composable
private fun DailyMindsetCard(
    streakDays: Int,
    totalMoments: Int,
    onNavigateToMemories: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = AshGrey),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_mindset_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Charcoal)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = Porcelain,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "DAILY REFLECTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Charcoal
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Porcelain,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Charcoal.copy(alpha = 0.15f)),
                    modifier = Modifier.clickable(onClick = onNavigateToMemories)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Memories",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Charcoal,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "“One photo each day turns ordinary time into a cherished story.”",
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                color = Charcoal,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Badges Row: Sandy Clay (Streak) + Sunlit Clay (Total Moments)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak Pill in Sandy Clay
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SandyClay,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Charcoal.copy(alpha = 0.18f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Charcoal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (streakDays > 0) "$streakDays Day Streak" else "Start Streak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal
                        )
                    }
                }

                // Total Moments Pill in Sunlit Clay
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SunlitClay,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Charcoal.copy(alpha = 0.18f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = Charcoal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$totalMoments Moments",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentPhotoThumbnailCard(
    photo: DailyPhoto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateLabel = remember(photo.journalDate) {
        try {
            val parsed = LocalDate.parse(photo.journalDate, DateTimeFormatter.ISO_LOCAL_DATE)
            parsed.format(DateTimeFormatter.ofPattern("MMM d"))
        } catch (_: Exception) {
            photo.journalDate
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .size(width = 115.dp, height = 155.dp)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.displayMediaModel)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.caption ?: "Recent moment",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video indicator
            if (photo.isVideo) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video moment",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Dark vignette bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                            startY = 60f
                        )
                    )
            )

            // Date chip at bottom
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = dateLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (!photo.mood.isNullOrEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(22.dp)
                ) {
                    Text(
                        text = getMoodEmoji(photo.mood),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMemoriesState(
    onTakePhoto: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No memories recorded yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Capture your first moment today or explore past dates in your calendar.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onTakePhoto,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Capture Today")
                }
                OutlinedButton(
                    onClick = onNavigateToCalendar,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calendar")
                }
            }
        }
    }
}

