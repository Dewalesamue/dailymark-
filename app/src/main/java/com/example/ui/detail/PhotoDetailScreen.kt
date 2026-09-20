package com.example.ui.detail

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DailyPhoto
import com.example.ui.components.MoodSelectorRow
import com.example.ui.components.getMoodEmoji
import com.example.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate

@Composable
fun PhotoDetailScreen(
    targetDate: String,
    initialPhotoId: String? = null,
    allPhotos: List<DailyPhoto>,
    today: LocalDate,
    onClose: () -> Unit,
    onUpdateCaptionAndMood: (DailyPhoto, String?, String?) -> Unit,
    onDeletePhoto: (DailyPhoto, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Find photos belonging strictly to this journal date
    val dayPhotos = remember(allPhotos, targetDate) {
        allPhotos.filter { it.journalDate == targetDate }.sortedBy { it.capturedAt }
    }

    if (dayPhotos.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "No memories for this date.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClose) {
                    Text("Back to journal")
                }
            }
        }
        return
    }

    val initialIndex = remember(dayPhotos, initialPhotoId) {
        val idx = dayPhotos.indexOfFirst { it.id == initialPhotoId }
        if (idx != -1) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, dayPhotos.size - 1),
        pageCount = { dayPhotos.size }
    )

    val currentPhoto = dayPhotos.getOrNull(pagerState.currentPage) ?: dayPhotos.first()

    val parsedDate = remember(targetDate) {
        DateTimeUtils.parseIsoDate(targetDate)
    }
    val isToday = remember(parsedDate, today) {
        parsedDate == today
    }

    val displayDateTitle = remember(parsedDate, targetDate) {
        if (parsedDate != null) DateTimeUtils.formatDisplayDate(parsedDate) else targetDate
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar: Navigation, Date Header, Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = displayDateTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isToday) "Today's moments" else "Remember this day",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (dayPhotos.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} of ${dayPhotos.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Photo Pager with Pinch-to-Zoom and fluid swipe navigation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("photo_viewer_pager")
                ) { page ->
                    val photo = dayPhotos[page]
                    ZoomablePhotoView(
                        photo = photo,
                        isCurrentPage = pagerState.currentPage == page,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Page indicator dots for multi-photo swiping
                if (dayPhotos.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(dayPhotos.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            // Bottom Panel: Caption, Mood, Actions
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Caption & Mood Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (!currentPhoto.caption.isNullOrBlank()) {
                                Text(
                                    text = currentPhoto.caption,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = "No caption added",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (!currentPhoto.mood.isNullOrEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = getMoodEmoji(currentPhoto.mood),
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons: Edit, Share, Save to device, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit Caption & Mood
                        DetailActionButton(
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            onClick = { showEditDialog = true },
                            testTag = "detail_edit_button"
                        )

                        // Share
                        DetailActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = {
                                sharePhoto(context, currentPhoto)
                            },
                            testTag = "detail_share_button"
                        )

                        // Save to device
                        DetailActionButton(
                            icon = Icons.Default.Download,
                            label = "Save",
                            onClick = {
                                savePhotoToGallery(context, currentPhoto)
                            },
                            testTag = "detail_save_button"
                        )

                        // Delete
                        DetailActionButton(
                            icon = Icons.Default.DeleteOutline,
                            label = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteDialog = true },
                            testTag = "detail_delete_button"
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this photo?") },
            text = { Text("This will permanently remove this moment from your journal.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePhoto(currentPhoto) {
                            if (dayPhotos.size <= 1) {
                                onClose()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit caption and mood dialog
    if (showEditDialog) {
        EditPhotoDialog(
            photo = currentPhoto,
            onDismiss = { showEditDialog = false },
            onConfirm = { newCaption, newMood ->
                onUpdateCaptionAndMood(currentPhoto, newCaption, newMood)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun ZoomablePhotoView(
    photo: DailyPhoto,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier
) {
    if (photo.isVideo) {
        VideoPlayerView(
            photo = photo,
            isCurrentPage = isCurrentPage,
            modifier = modifier
        )
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    val transformModifier = if (scale > 1.05f) {
        Modifier.pointerInput(scale) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 4f)
                if (scale > 1.05f) {
                    val maxPanX = (scale - 1f) * 400f
                    val maxPanY = (scale - 1f) * 400f
                    offset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                        y = (offset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                    )
                } else {
                    offset = Offset.Zero
                }
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    }
                )
            }
            .then(transformModifier),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.displayMediaModel)
                .crossfade(true)
                .build(),
            contentDescription = photo.caption ?: "Memory photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

@Composable
private fun VideoPlayerView(
    photo: DailyPhoto,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<android.widget.VideoView?>(null) }

    DisposableEffect(isCurrentPage) {
        if (!isCurrentPage) {
            videoViewRef?.pause()
            isPlaying = false
        }
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                videoViewRef?.let { vv ->
                    if (vv.isPlaying) {
                        vv.pause()
                        isPlaying = false
                    } else {
                        vv.start()
                        isPlaying = true
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                android.widget.VideoView(ctx).apply {
                    val mediaUri = if (File(photo.filePath).exists()) {
                        Uri.fromFile(File(photo.filePath))
                    } else if (!photo.remoteUrl.isNullOrEmpty()) {
                        Uri.parse(photo.remoteUrl)
                    } else {
                        null
                    }
                    if (mediaUri != null) {
                        setVideoURI(mediaUri)
                    }
                    setOnPreparedListener { mp ->
                        isPrepared = true
                        mp.isLooping = true
                        if (isCurrentPage) {
                            start()
                            isPlaying = true
                        }
                    }
                    setOnCompletionListener {
                        isPlaying = false
                    }
                    setOnErrorListener { _, _, _ ->
                        true
                    }
                    videoViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay play button if paused
        if (!isPlaying && isPrepared) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .size(64.dp)
                    .clickable {
                        videoViewRef?.let { vv ->
                            vv.start()
                            isPlaying = true
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EditPhotoDialog(
    photo: DailyPhoto,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?) -> Unit
) {
    var caption by remember { mutableStateOf(photo.caption ?: "") }
    var selectedMood by remember { mutableStateOf(photo.mood) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Moment", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Caption",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Add caption...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Mood",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                MoodSelectorRow(
                    selectedMood = selectedMood,
                    onMoodSelected = { selectedMood = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(caption, selectedMood) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun sharePhoto(context: Context, photo: DailyPhoto) {
    try {
        val file = File(photo.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Media file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = if (photo.isVideo) "video/mp4" else "image/jpeg"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, photo.caption ?: "Memory from ${photo.journalDate}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, if (photo.isVideo) "Share Video" else "Share Memory"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share media", Toast.LENGTH_SHORT).show()
    }
}

private fun savePhotoToGallery(context: Context, photo: DailyPhoto) {
    try {
        val srcFile = File(photo.filePath)
        if (!srcFile.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        val isVid = photo.isVideo
        val ext = if (isVid) "mp4" else "jpg"
        val mimeType = if (isVid) "video/mp4" else "image/jpeg"
        val relPath = if (isVid) Environment.DIRECTORY_MOVIES + "/OnePhoto" else Environment.DIRECTORY_PICTURES + "/OnePhoto"
        val targetUri = if (isVid) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Journal_${photo.journalDate}_${System.currentTimeMillis()}.$ext")
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri: Uri? = contentResolver.insert(targetUri, contentValues)
        if (imageUri != null) {
            contentResolver.openOutputStream(imageUri)?.use { out ->
                FileInputStream(srcFile).use { input ->
                    input.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
            }
            val destination = if (isVid) "Movies/OnePhoto" else "Pictures/OnePhoto"
            Toast.makeText(context, "Saved to $destination", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save media", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving media: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
