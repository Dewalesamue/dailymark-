package com.example.ui.calendar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DailyPhoto
import com.example.ui.JournalUiState
import com.example.ui.components.DateActionSheet
import com.example.ui.components.getMoodEmoji
import com.example.ui.theme.AshGrey
import com.example.ui.theme.Charcoal
import com.example.ui.theme.Porcelain
import com.example.ui.theme.SandyClay
import com.example.ui.theme.SunlitClay
import com.example.util.DateTimeUtils
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: JournalUiState,
    onNavigateMonth: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onCaptureForDate: (LocalDate) -> Unit,
    onOpenPhotoDetail: (String) -> Unit,
    onPickForDate: (LocalDate, List<Uri>) -> Unit,
    onDismissActionSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val month = uiState.currentMonth
    val today = remember(uiState.configuredTimeZone) {
        DateTimeUtils.getToday(uiState.configuredTimeZone)
    }

    val photosByDate = remember(uiState.photos) {
        uiState.photos.groupBy { it.journalDate }
    }

    val selectedDate = uiState.selectedDate
    val selectedDateStr = DateTimeUtils.toIsoDate(selectedDate)
    val selectedPhotos = photosByDate[selectedDateStr] ?: emptyList()

    val isSelectedToday = selectedDate == today
    val isSelectedFuture = selectedDate.isAfter(today)
    val isSelectedPast = selectedDate.isBefore(today)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onPickForDate(selectedDate, uris)
        }
    }

    // Days in current month
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfMonth = month.atDay(1).dayOfWeek
    val firstDayOffset = (firstDayOfMonth.value % 7) // Sunday = 0, Monday = 1...

    val totalCells = (firstDayOffset + daysInMonth + 6) / 7 * 7
    val rows = totalCells / 7

    val prefix = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val photosInCurrentMonth = remember(uiState.photos, month) {
        uiState.photos.filter { it.journalDate.startsWith(prefix) }
    }
    val daysWithPhotosInMonth = remember(photosInCurrentMonth) {
        photosInCurrentMonth.map { it.journalDate }.distinct().size
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Month Navigation Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$daysWithPhotosInMonth of $daysInMonth days captured (${photosInCurrentMonth.size} moments)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SandyClay,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable(onClick = onJumpToToday)
                            .testTag("jump_to_today_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = "Jump to Today",
                                tint = Charcoal,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Today",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Charcoal
                            )
                        }
                    }

                    IconButton(
                        onClick = { onNavigateMonth(-1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AshGrey.copy(alpha = 0.25f))
                            .testTag("prev_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = Charcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { onNavigateMonth(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AshGrey.copy(alpha = 0.25f))
                            .testTag("next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = Charcoal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Calendar Grid Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AshGrey.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .testTag("calendar_grid_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Day of week headers
                    val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        dayNames.forEach { name ->
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AshGrey,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Month Rows
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - firstDayOffset + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = month.atDay(dayNumber)
                                    val dateStr = DateTimeUtils.toIsoDate(cellDate)
                                    val photos = photosByDate[dateStr] ?: emptyList()
                                    val isToday = cellDate == today
                                    val isFuture = cellDate.isAfter(today)
                                    val isSelected = cellDate == selectedDate

                                    CalendarDayCell(
                                        dayNumber = dayNumber,
                                        photos = photos,
                                        isToday = isToday,
                                        isFuture = isFuture,
                                        isSelected = isSelected,
                                        onClick = { onSelectDate(cellDate) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Panel
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .testTag("selected_day_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = DateTimeUtils.formatDisplayDate(selectedDate),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelectedToday) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SandyClay
                                    ) {
                                        Text(
                                            text = "Today",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Charcoal,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = when {
                                    isSelectedFuture -> "Not yet. That day hasn't happened yet."
                                    isSelectedToday -> "Capture today."
                                    else -> "Remember this day."
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }

                        if (!isSelectedFuture) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AshGrey.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AshGrey.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${selectedPhotos.size} moments",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Charcoal,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when {
                        // FUTURE DATE PANEL: Disabled, informative, no buttons
                        isSelectedFuture -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AshGrey.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AshGrey.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = AshGrey,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "That day hasn't happened yet.",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Come back when the day arrives.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // TODAY OR PAST WITH PHOTOS
                        selectedPhotos.isNotEmpty() -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(selectedPhotos) { photo ->
                                    Box(
                                        modifier = Modifier
                                            .size(width = 86.dp, height = 100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { onOpenPhotoDetail(photo.id) }
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(photo.displayMediaModel)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = photo.caption ?: "Moment",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (photo.isVideo) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.55f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Video",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (!photo.mood.isNullOrEmpty()) {
                                            Surface(
                                                shape = CircleShape,
                                                color = SandyClay,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(18.dp)
                                            ) {
                                                Text(
                                                    text = getMoodEmoji(photo.mood),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons: Take Photo (Charcoal) and Gallery (Sunlit Clay)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isSelectedToday) {
                                    Button(
                                        onClick = { onCaptureForDate(selectedDate) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Charcoal,
                                            contentColor = Porcelain
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Take Photo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SunlitClay,
                                        contentColor = Charcoal
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSelectedToday) "Add from Gallery" else "Add Memories",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // EMPTY STATE (TODAY OR PAST WITH NO PHOTOS)
                        else -> {
                            if (isSelectedPast) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AshGrey.copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AshGrey.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "You can add existing photos and videos to past days.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Charcoal,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SandyClay,
                                        contentColor = Charcoal
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Memories", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // Empty state for TODAY: Camera in Charcoal, Gallery in Sandy Clay
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { onCaptureForDate(selectedDate) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Charcoal,
                                            contentColor = Porcelain
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Take Photo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            galleryLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SandyClay,
                                            contentColor = Charcoal
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("From Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Date Action Sheet when triggered
    if (uiState.activeDateActionSheet != null) {
        val targetDate = uiState.activeDateActionSheet
        val targetDateStr = DateTimeUtils.toIsoDate(targetDate)
        val photosForTarget = photosByDate[targetDateStr] ?: emptyList()

        DateActionSheet(
            targetDate = targetDate,
            today = today,
            photos = photosForTarget,
            onDismiss = onDismissActionSheet,
            onTakePhoto = { onCaptureForDate(targetDate) },
            onPickFromGallery = { uris -> onPickForDate(targetDate, uris) },
            onViewMemories = {
                val firstPhotoId = photosForTarget.firstOrNull()?.id
                if (firstPhotoId != null) {
                    onOpenPhotoDetail(firstPhotoId)
                }
            }
        )
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    photos: List<DailyPhoto>,
    isToday: Boolean,
    isFuture: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isSelected -> SunlitClay
        isToday -> SandyClay
        else -> AshGrey.copy(alpha = 0.25f)
    }

    val borderWidth = when {
        isSelected -> 2.dp
        isToday -> 2.dp
        else -> 0.5.dp
    }

    val hasPhotos = photos.isNotEmpty()

    val cellBackground = when {
        isFuture -> AshGrey.copy(alpha = 0.12f)
        hasPhotos -> Color.Black.copy(alpha = 0.05f)
        isToday -> SunlitClay.copy(alpha = 0.22f)
        else -> AshGrey.copy(alpha = 0.16f)
    }

    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(10.dp))
            .background(cellBackground)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("calendar_day_$dayNumber")
    ) {
        when {
            // FUTURE STATE: Muted Ash Grey, lock icon
            isFuture -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$dayNumber",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AshGrey.copy(alpha = 0.7f)
                    )

                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AshGrey.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // HAS PHOTOS (TODAY OR PAST)
            hasPhotos -> {
                val firstPhoto = photos.first()

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(firstPhoto.displayMediaModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (firstPhoto.isVideo) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Vignette for day number readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )

                // Day number
                Text(
                    text = "$dayNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                )

                // Photos count pill if multiple (Sandy Clay)
                if (photos.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SandyClay,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                    ) {
                        Text(
                            text = "${photos.size}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // TODAY WITH NO PHOTOS: Sunlit Clay background + Sandy Clay dot + Charcoal text
            isToday -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$dayNumber",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SandyClay)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture today",
                        tint = Charcoal,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = "Today",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Charcoal
                    )
                }
            }

            // PAST DATE WITH NO PHOTOS: Subtle Ash Grey with Charcoal number and Ash Grey add icon
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$dayNumber",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Charcoal.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add memory",
                        tint = AshGrey,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}
