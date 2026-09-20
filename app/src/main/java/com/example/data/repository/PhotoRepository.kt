package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import com.example.data.db.PhotoDao
import com.example.data.model.DailyPhoto
import com.example.data.supabase.SupabaseService
import com.example.data.supabase.SupabaseRemotePhoto
import com.example.util.DateTimeUtils
import com.example.util.ExifHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class JournalStats(
    val totalMoments: Int = 0,
    val firstDate: String? = null,
    val mostActiveMonth: String? = null
)

class PhotoRepository(
    private val context: Context,
    private val photoDao: PhotoDao,
    val supabaseService: SupabaseService
) {
    fun getAllPhotos(userId: String): Flow<List<DailyPhoto>> = photoDao.getAllPhotos(userId)

    fun getPhotosByDate(userId: String, journalDate: String): Flow<List<DailyPhoto>> =
        photoDao.getPhotosByDate(userId, journalDate)

    suspend fun getPhotosByDateSync(userId: String, journalDate: String): List<DailyPhoto> =
        photoDao.getPhotosByDateSync(userId, journalDate)

    fun getPhotoByDate(userId: String, journalDate: String): Flow<DailyPhoto?> =
        photoDao.getPhotoByDate(userId, journalDate)

    suspend fun getPhotoByDateSync(userId: String, journalDate: String): DailyPhoto? =
        photoDao.getPhotoByDateSync(userId, journalDate)

    suspend fun getPhotoCountForDate(userId: String, journalDate: String): Int =
        photoDao.getPhotoCountForDate(userId, journalDate)

    fun getPhotosForMonth(userId: String, yearMonth: String): Flow<List<DailyPhoto>> =
        photoDao.getPhotosForMonth(userId, yearMonth)

    fun getPhotoById(userId: String, id: String): Flow<DailyPhoto?> =
        photoDao.getPhotoById(userId, id)

    /**
     * Saves a photo to local storage and database, syncing to Supabase if configured.
     * Scoped strictly to the authenticated userId.
     */
    suspend fun savePhoto(
        bitmap: Bitmap? = null,
        uri: Uri? = null,
        journalDate: LocalDate,
        userId: String,
        caption: String? = null,
        mood: String? = null,
        userTimeZone: String? = null,
        capturedAtOverride: Long? = null
    ): Result<DailyPhoto> = withContext(Dispatchers.IO) {
        try {
            val today = DateTimeUtils.getToday(userTimeZone)
            if (journalDate.isAfter(today)) {
                return@withContext Result.failure(
                    IllegalStateException("Future journal dates are not allowed.")
                )
            }

            val dateStr = DateTimeUtils.toIsoDate(journalDate) // "yyyy-MM-dd"
            val photosDir = File(context.filesDir, "photos").apply {
                if (!exists()) mkdirs()
            }

            val photoId = UUID.randomUUID().toString()
            val nowUtc = System.currentTimeMillis()
            val targetFile = File(photosDir, "photo_${dateStr}_${nowUtc}_${photoId.take(6)}.jpg")

            // Process and save bitmap
            val processedBitmap: Bitmap = when {
                bitmap != null -> bitmap
                uri != null -> decodeUriToBitmap(uri) ?: return@withContext Result.failure(Exception("Failed to decode image"))
                else -> return@withContext Result.failure(Exception("No image data provided"))
            }

            FileOutputStream(targetFile).use { out ->
                processedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }

            // Extract EXIF captured date/time if available
            val exifData = if (uri != null) {
                ExifHelper.extractExifData(context, uri, today)
            } else null

            val capturedAt = capturedAtOverride
                ?: exifData?.captureTimeMillis
                ?: nowUtc

            // Cloud storage upload
            val uploadResult = supabaseService.uploadMedia(userId, targetFile, "photo")
            val cloudUrl = uploadResult.getOrNull()

            val newPhoto = DailyPhoto(
                id = photoId,
                userId = userId,
                storagePath = targetFile.absolutePath,
                photoUrl = cloudUrl,
                journalDate = dateStr,
                capturedAt = capturedAt,
                caption = caption?.trim()?.ifEmpty { null },
                mood = mood?.trim()?.ifEmpty { null },
                width = processedBitmap.width,
                height = processedBitmap.height,
                mediaType = DailyPhoto.MEDIA_TYPE_PHOTO,
                createdAt = nowUtc,
                updatedAt = nowUtc
            )

            photoDao.insertOrUpdate(newPhoto)
            supabaseService.upsertPhoto(newPhoto)

            Result.success(newPhoto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves a recorded or selected video to local storage and database,
     * syncing to Supabase Storage and Postgres.
     */
    suspend fun saveVideo(
        uri: Uri,
        journalDate: LocalDate,
        userId: String,
        caption: String? = null,
        mood: String? = null,
        userTimeZone: String? = null
    ): Result<DailyPhoto> = withContext(Dispatchers.IO) {
        try {
            val today = DateTimeUtils.getToday(userTimeZone)
            if (journalDate.isAfter(today)) {
                return@withContext Result.failure(
                    IllegalStateException("Future journal dates are not allowed.")
                )
            }

            val dateStr = DateTimeUtils.toIsoDate(journalDate)
            val videosDir = File(context.filesDir, "videos").apply {
                if (!exists()) mkdirs()
            }

            val videoId = UUID.randomUUID().toString()
            val nowUtc = System.currentTimeMillis()
            val targetFile = File(videosDir, "video_${dateStr}_${nowUtc}_${videoId.take(6)}.mp4")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Failed to read video stream"))

            // Extract video dimensions if possible
            var width: Int? = null
            var height: Int? = null
            try {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(targetFile.absolutePath)
                val w = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val h = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                if (rotation == 90 || rotation == 270) {
                    width = h
                    height = w
                } else {
                    width = w
                    height = h
                }
                retriever.release()
            } catch (_: Exception) {}

            // Cloud upload to Supabase Storage
            val uploadResult = supabaseService.uploadMedia(userId, targetFile, "video")
            val cloudUrl = uploadResult.getOrNull()

            val newVideo = DailyPhoto(
                id = videoId,
                userId = userId,
                storagePath = targetFile.absolutePath,
                photoUrl = cloudUrl,
                journalDate = dateStr,
                capturedAt = nowUtc,
                caption = caption?.trim()?.ifEmpty { null },
                mood = mood?.trim()?.ifEmpty { null },
                width = width ?: 1080,
                height = height ?: 1920,
                mediaType = DailyPhoto.MEDIA_TYPE_VIDEO,
                createdAt = nowUtc,
                updatedAt = nowUtc
            )

            photoDao.insertOrUpdate(newVideo)
            supabaseService.upsertPhoto(newVideo)

            Result.success(newVideo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves multiple photos to the local storage and database.
     * Enforces the business logic rule: journal_date <= today in user's timezone.
     * Unlimited photos per journal date.
     */
    suspend fun saveMultiplePhotos(
        uris: List<Uri>,
        journalDate: LocalDate,
        userId: String,
        userTimeZone: String? = null,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<List<DailyPhoto>> = withContext(Dispatchers.IO) {
        try {
            val today = DateTimeUtils.getToday(userTimeZone)
            if (journalDate.isAfter(today)) {
                return@withContext Result.failure(
                    IllegalStateException("Future journal dates are not allowed.")
                )
            }

            val dateStr = DateTimeUtils.toIsoDate(journalDate)
            val savedList = mutableListOf<DailyPhoto>()
            val photosDir = File(context.filesDir, "photos").apply {
                if (!exists()) mkdirs()
            }
            val nowUtc = System.currentTimeMillis()

            for (index in uris.indices) {
                val uri = uris[index]
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val isVideo = mimeType.startsWith("video/")

                val photoId = UUID.randomUUID().toString()

                if (isVideo) {
                    val videosDir = File(context.filesDir, "videos").apply { if (!exists()) mkdirs() }
                    val targetFile = File(videosDir, "video_${dateStr}_${nowUtc}_${index}_${photoId.take(4)}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val uploadResult = supabaseService.uploadMedia(userId, targetFile, "video")
                    val videoPhoto = DailyPhoto(
                        id = photoId,
                        userId = userId,
                        storagePath = targetFile.absolutePath,
                        photoUrl = uploadResult.getOrNull(),
                        journalDate = dateStr,
                        capturedAt = nowUtc + index,
                        caption = null,
                        mood = null,
                        width = 1080,
                        height = 1920,
                        mediaType = DailyPhoto.MEDIA_TYPE_VIDEO,
                        createdAt = nowUtc,
                        updatedAt = nowUtc
                    )
                    photoDao.insertOrUpdate(videoPhoto)
                    supabaseService.upsertPhoto(videoPhoto)
                    savedList.add(videoPhoto)
                } else {
                    val decoded = decodeUriToBitmap(uri) ?: continue
                    val targetFile = File(photosDir, "photo_${dateStr}_${nowUtc}_${index}_${photoId.take(4)}.jpg")

                    FileOutputStream(targetFile).use { out ->
                        decoded.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        out.flush()
                    }

                    val exifData = ExifHelper.extractExifData(context, uri, today)
                    val capturedAt = exifData.captureTimeMillis ?: (nowUtc + index)

                    val uploadResult = supabaseService.uploadMedia(userId, targetFile, "photo")

                    val photo = DailyPhoto(
                        id = photoId,
                        userId = userId,
                        storagePath = targetFile.absolutePath,
                        photoUrl = uploadResult.getOrNull(),
                        journalDate = dateStr,
                        capturedAt = capturedAt,
                        caption = null,
                        mood = null,
                        width = decoded.width,
                        height = decoded.height,
                        mediaType = DailyPhoto.MEDIA_TYPE_PHOTO,
                        createdAt = nowUtc,
                        updatedAt = nowUtc
                    )

                    photoDao.insertOrUpdate(photo)
                    supabaseService.upsertPhoto(photo)
                    savedList.add(photo)
                }
                onProgress(index + 1, uris.size)
            }

            Result.success(savedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pulls memories from Supabase Cloud for this user and populates local Room DB.
     * Ensures memories persist across uninstalls/reinstalls.
     */
    suspend fun syncFromCloud(userId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!supabaseService.isConfigured || userId.isBlank()) {
                return@withContext Result.success(0)
            }
            val remotePhotosResult = supabaseService.fetchPhotos(userId)
            val remotePhotos = remotePhotosResult.getOrNull()
                ?: return@withContext Result.failure(remotePhotosResult.exceptionOrNull() ?: Exception("Sync failed"))

            val localEntities = remotePhotos.map { it.toDailyPhoto() }
            if (localEntities.isNotEmpty()) {
                photoDao.insertAll(localEntities)
            }
            Result.success(localEntities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePhoto(photo: DailyPhoto) = withContext(Dispatchers.IO) {
        val updated = photo.copy(updatedAt = System.currentTimeMillis())
        photoDao.insertOrUpdate(updated)
        supabaseService.upsertPhoto(updated)
    }

    suspend fun deletePhoto(photo: DailyPhoto, userId: String = photo.userId) = withContext(Dispatchers.IO) {
        try {
            val file = File(photo.storagePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        photoDao.deletePhoto(photo)
        supabaseService.deletePhoto(photo.id, userId)
        try {
            val fileName = File(photo.storagePath).name
            supabaseService.deleteMedia(userId, fileName)
        } catch (_: Exception) {}
    }

    private fun decodeUriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (original != null) {
                val exifStream = context.contentResolver.openInputStream(uri)
                val exif = exifStream?.let { ExifInterface(it) }
                val orientation = exif?.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                ) ?: ExifInterface.ORIENTATION_NORMAL
                exifStream?.close()

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun calculateStats(photos: List<DailyPhoto>, today: LocalDate = LocalDate.now()): JournalStats {
            if (photos.isEmpty()) {
                return JournalStats()
            }

            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val dates = photos.mapNotNull {
                try { LocalDate.parse(it.journalDate, formatter) } catch (_: Exception) { null }
            }.toSet()

            val sortedDates = dates.sorted()
            val firstDate = sortedDates.firstOrNull()?.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

            val monthCounts = photos.groupingBy {
                it.journalDate.take(7) // "yyyy-MM"
            }.eachCount()
            val topMonthKey = monthCounts.maxByOrNull { it.value }?.key
            val mostActiveMonth = topMonthKey?.let {
                try {
                    val parts = it.split("-")
                    val year = parts[0]
                    val monthNum = parts[1].toInt()
                    val monthName = java.time.Month.of(monthNum).name.lowercase().replaceFirstChar { char -> char.uppercase() }
                    "$monthName $year"
                } catch (_: Exception) {
                    it
                }
            }

            return JournalStats(
                totalMoments = photos.size,
                firstDate = firstDate,
                mostActiveMonth = mostActiveMonth
            )
        }
    }
}
