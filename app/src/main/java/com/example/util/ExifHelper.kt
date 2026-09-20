package com.example.util

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ExifMetadataResult(
    val captureTimeMillis: Long? = null,
    val suggestedDate: LocalDate? = null
)

object ExifHelper {
    private val EXIF_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")

    fun extractExifData(context: Context, uri: Uri, today: LocalDate): ExifMetadataResult {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return ExifMetadataResult()
            val exif = try {
                ExifInterface(inputStream)
            } finally {
                inputStream.close()
            }

            val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

            if (!dateStr.isNullOrBlank()) {
                val parsedDateTime = try {
                    LocalDateTime.parse(dateStr.trim(), EXIF_FORMATTER)
                } catch (_: Exception) {
                    null
                }

                if (parsedDateTime != null) {
                    val parsedDate = parsedDateTime.toLocalDate()
                    val epochMillis = try {
                        parsedDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } catch (_: Exception) {
                        null
                    }

                    // Strict Policy: EXIF metadata must NEVER allow a photo to be assigned to a future journal date
                    val safeSuggestedDate = if (parsedDate.isAfter(today)) {
                        null
                    } else {
                        parsedDate
                    }

                    return ExifMetadataResult(
                        captureTimeMillis = epochMillis,
                        suggestedDate = safeSuggestedDate
                    )
                }
            }

            ExifMetadataResult()
        } catch (_: Exception) {
            ExifMetadataResult()
        }
    }
}
