package com.example.ui.memories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DailyPhoto
import com.example.ui.JournalUiState
import com.example.ui.components.AvailableMoods
import com.example.ui.components.MoodChip
import com.example.ui.components.getMoodEmoji
import com.example.ui.theme.AshGrey
import com.example.ui.theme.Charcoal
import com.example.ui.theme.Porcelain
import com.example.ui.theme.SandyClay
import com.example.ui.theme.SunlitClay
import com.example.util.DateTimeUtils
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MemoriesScreen(
    uiState: JournalUiState,
    today: LocalDate,
    onOpenPhotoDetail: (photoId: String, dateStr: String) -> Unit,
    onUpdateCustomName: (date: String, title: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMoodFilter by remember { mutableStateOf<String?>(null) }
    var selectedDateForDetail by remember { mutableStateOf<String?>(null) }
    var editingCustomNameDate by remember { mutableStateOf<String?>(null) }

    // If a date is selected to view all its photos, show the DatePhotosView screen
    if (selectedDateForDetail != null) {
        val dateStr = selectedDateForDetail!!
        val photosForDate = remember(uiState.photos, dateStr) {
            uiState.photos.filter { it.journalDate == dateStr }.sortedBy { it.capturedAt }
        }

        // Intercept back to return to the Date Buckets list
        BackHandler {
            selectedDateForDetail = null
        }

        DatePhotosView(
            dateStr = dateStr,
            photos = photosForDate,
            customName = uiState.dayCustomTitles[dateStr],
            today = today,
            onBack = { selectedDateForDetail = null },
            onOpenPhoto = { photoId -> onOpenPhotoDetail(photoId, dateStr) },
            onEditName = { editingCustomNameDate = dateStr },
            modifier = modifier
        )
    } else {
        // Group photos by journalDate (each calendar date is its own bucket)
        val filteredPhotos = remember(uiState.photos, searchQuery, selectedMoodFilter, uiState.dayCustomTitles) {
            uiState.photos.filter { photo ->
                val customTitle = uiState.dayCustomTitles[photo.journalDate]
                val matchesQuery = searchQuery.isBlank() ||
                        (photo.caption?.contains(searchQuery, ignoreCase = true) == true) ||
                        photo.journalDate.contains(searchQuery) ||
                        (customTitle?.contains(searchQuery, ignoreCase = true) == true)
                val matchesMood = selectedMoodFilter == null ||
                        photo.mood.equals(selectedMoodFilter, ignoreCase = true)
                matchesQuery && matchesMood
            }
        }

        val dateBuckets = remember(filteredPhotos) {
            filteredPhotos
                .groupBy { it.journalDate }
                .toList()
                .sortedByDescending { it.first } // Newest dates first
        }

        LazyColumn(
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize()
        ) {
            // Screen Header
            item {
                Column {
                    Text(
                        text = "Memories",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Day-based archive of your visual journey",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by day name, caption, or date...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memories_search_field")
                )
            }

            // Mood Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        val isAll = selectedMoodFilter == null
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isAll) Charcoal else AshGrey.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isAll) 1.5.dp else 1.dp,
                                if (isAll) Charcoal else AshGrey.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedMoodFilter = null }
                        ) {
                            Text(
                                text = "All Moments",
                                fontSize = 13.sp,
                                fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAll) Porcelain else Charcoal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    items(AvailableMoods) { (name, _) ->
                        val isSelected = selectedMoodFilter.equals(name, ignoreCase = true)
                        MoodChip(
                            moodName = name,
                            isSelected = isSelected,
                            onSelect = {
                                selectedMoodFilter = if (isSelected) null else name
                            }
                        )
                    }
                }
            }

            // Empty State
            if (dateBuckets.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedMoodFilter != null)
                                    "No memories match your filter."
                                else
                                    "No memories captured yet.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Every photo you capture is organized into its calendar date bucket.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Day-based Buckets
                items(dateBuckets, key = { it.first }) { (dateStr, photos) ->
                    val customName = uiState.dayCustomTitles[dateStr]
                    DateBucketCard(
                        dateStr = dateStr,
                        photos = photos,
                        customName = customName,
                        today = today,
                        onClick = { selectedDateForDetail = dateStr },
                        onEditName = { editingCustomNameDate = dateStr }
                    )
                }
            }
        }
    }

    // Dialog to set or update optional custom name for that day
    editingCustomNameDate?.let { dateToEdit ->
        val currentName = uiState.dayCustomTitles[dateToEdit] ?: ""
        DayCustomNameDialog(
            dateStr = dateToEdit,
            currentName = currentName,
            today = today,
            onDismiss = { editingCustomNameDate = null },
            onSave = { newName ->
                onUpdateCustomName(dateToEdit, newName)
                editingCustomNameDate = null
            }
        )
    }
}

/**
 * A Day Bucket representing one calendar date. Holds MANY photos, shows the real date
 * alongside an optional custom name (real date is never replaced), and previews multiple photos.
 */
@Composable
private fun DateBucketCard(
    dateStr: String,
    photos: List<DailyPhoto>,
    customName: String?,
    today: LocalDate,
    onClick: () -> Unit,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedDate = remember(dateStr) { DateTimeUtils.parseIsoDate(dateStr) }
    val dayLabel = remember(parsedDate, today, dateStr) {
        if (parsedDate != null) DateTimeUtils.formatDayBucketLabel(parsedDate, today) else dateStr
    }
    val fullDateString = remember(parsedDate, dateStr) {
        if (parsedDate != null) DateTimeUtils.formatFullDayDate(parsedDate) else dateStr
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AshGrey.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("date_bucket_$dateStr")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Bucket Header: Real Date always visible, custom name alongside, edit button & photo count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (!customName.isNullOrBlank()) {
                        // Custom Name is prominent
                        Text(
                            text = customName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Real date remains visible alongside it, never replaced
                        Text(
                            text = "$dayLabel • $fullDateString",
                            fontSize = 12.sp,
                            color = SunlitClay,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        // No custom name: display real date prominently
                        Text(
                            text = dayLabel,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = fullDateString,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit custom name button
                    IconButton(
                        onClick = onEditName,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_day_name_$dateStr")
                    ) {
                        Icon(
                            imageVector = if (!customName.isNullOrBlank()) Icons.Default.Edit else Icons.Default.Label,
                            contentDescription = "Edit Day Name",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Photo count badge (showing it holds MANY photos)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SandyClay
                    ) {
                        Text(
                            text = if (photos.size == 1) "1 photo" else "${photos.size} photos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Charcoal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-Photo Preview Collage
            MultiPhotoCollagePreview(
                photos = photos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )

            // Mood highlight preview if available
            val moods = remember(photos) { photos.mapNotNull { it.mood }.distinct().take(4) }
            if (moods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Moods:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                    moods.forEach { moodName ->
                        val emoji = getMoodEmoji(moodName)
                        Surface(
                            shape = CircleShape,
                            color = AshGrey.copy(alpha = 0.25f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual collage showing that a bucket holds multiple photos.
 */
@Composable
private fun MultiPhotoCollagePreview(
    photos: List<DailyPhoto>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = modifier.clip(RoundedCornerShape(14.dp))
    ) {
        when {
            photos.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No photos", fontSize = 12.sp, color = Color.Gray)
                }
            }

            photos.size == 1 -> {
                // 1 photo: Single full bleed
                ThumbnailImage(photo = photos[0], modifier = Modifier.fillMaxSize())
            }

            photos.size == 2 -> {
                // 2 photos: Split horizontally
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThumbnailImage(photo = photos[0], modifier = Modifier.weight(1f).fillMaxSize())
                    ThumbnailImage(photo = photos[1], modifier = Modifier.weight(1f).fillMaxSize())
                }
            }

            photos.size == 3 -> {
                // 3 photos: 1 featured on left, 2 stacked on right
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThumbnailImage(photo = photos[0], modifier = Modifier.weight(1.3f).fillMaxSize())
                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ThumbnailImage(photo = photos[1], modifier = Modifier.weight(1f).fillMaxSize())
                        ThumbnailImage(photo = photos[2], modifier = Modifier.weight(1f).fillMaxSize())
                    }
                }
            }

            else -> {
                // 4+ photos: 1 featured on left, 2 stacked on right with "+N more" badge on bottom-right
                val extraCount = photos.size - 3
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThumbnailImage(photo = photos[0], modifier = Modifier.weight(1.3f).fillMaxSize())
                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ThumbnailImage(photo = photos[1], modifier = Modifier.weight(1f).fillMaxSize())
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            ThumbnailImage(photo = photos[2], modifier = Modifier.fillMaxSize())
                            if (extraCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$extraCount",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailImage(
    photo: DailyPhoto,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.displayMediaModel)
                .crossfade(true)
                .build(),
            contentDescription = photo.caption ?: "Moment photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (photo.isVideo) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Screen showing ALL photos under a specific date bucket.
 * Opened when tapping any date bucket in Memories.
 */
@Composable
private fun DatePhotosView(
    dateStr: String,
    photos: List<DailyPhoto>,
    customName: String?,
    today: LocalDate,
    onBack: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onEditName: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedDate = remember(dateStr) { DateTimeUtils.parseIsoDate(dateStr) }
    val dayLabel = remember(parsedDate, today, dateStr) {
        if (parsedDate != null) DateTimeUtils.formatDayBucketLabel(parsedDate, today) else dateStr
    }
    val fullDateString = remember(parsedDate, dateStr) {
        if (parsedDate != null) DateTimeUtils.formatFullDayDate(parsedDate) else dateStr
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("date_photos_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Memories",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (!customName.isNullOrBlank()) {
                    Text(
                        text = customName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$dayLabel • $fullDateString",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = dayLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = fullDateString,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Edit custom day name button
            OutlinedButton(
                onClick = onEditName,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (customName.isNullOrBlank()) "Name day" else "Rename",
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${photos.size} moments captured on this day",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // Grid showing all photos under this date
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(photos, key = { it.id }) { photo ->
                DatePhotoGridItem(
                    photo = photo,
                    onClick = { onOpenPhoto(photo.id) }
                )
            }
        }
    }
}

@Composable
private fun DatePhotoGridItem(
    photo: DailyPhoto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedTime = remember(photo.capturedAt) {
        try {
            val instant = Instant.ofEpochMilli(photo.capturedAt)
            val zdt = instant.atZone(ZoneId.systemDefault())
            zdt.format(DateTimeFormatter.ofPattern("h:mm a"))
        } catch (_: Exception) {
            ""
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("photo_item_${photo.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.displayMediaModel)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.caption ?: "Moment photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

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

            // Gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                            startY = 100f
                        )
                    )
            )

            // Mood chip at top
            if (!photo.mood.isNullOrEmpty()) {
                val emoji = getMoodEmoji(photo.mood)
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = emoji,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            // Time & caption at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                if (formattedTime.isNotBlank()) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                if (!photo.caption.isNullOrEmpty()) {
                    Text(
                        text = photo.caption,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Dialog to set or edit the optional custom day name.
 * Real date stays visible and is never replaced.
 */
@Composable
private fun DayCustomNameDialog(
    dateStr: String,
    currentName: String,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var nameText by remember { mutableStateOf(currentName) }
    val parsedDate = remember(dateStr) { DateTimeUtils.parseIsoDate(dateStr) }
    val dateLabel = remember(parsedDate, today, dateStr) {
        if (parsedDate != null) DateTimeUtils.formatDayBucketLabel(parsedDate, today) else dateStr
    }
    val fullDateString = remember(parsedDate, dateStr) {
        if (parsedDate != null) DateTimeUtils.formatFullDayDate(parsedDate) else dateStr
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Name This Day", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = "$dateLabel ($fullDateString)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Give this day a special title. The calendar date stays visible alongside it.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    placeholder = { Text("e.g. Summer Road Trip, Birthday Party") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameText.trim().ifEmpty { null }) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (currentName.isNotBlank()) {
                    TextButton(onClick = { onSave(null) }) {
                        Text("Remove Name", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
