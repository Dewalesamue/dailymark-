package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    indices = [Index(value = ["journal_date"], unique = false)]
)
data class DailyPhoto(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String = "local_user",
    @ColumnInfo(name = "storage_path") val storagePath: String,
    @ColumnInfo(name = "photo_url") val photoUrl: String? = null,
    @ColumnInfo(name = "journal_date") val journalDate: String, // Format "yyyy-MM-dd"
    @ColumnInfo(name = "captured_at") val capturedAt: Long = System.currentTimeMillis(),
    val caption: String? = null,
    val mood: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @ColumnInfo(name = "media_type") val mediaType: String = MEDIA_TYPE_PHOTO,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    // Backwards-compatible aliases to ease usage across UI components
    val localDate: String get() = journalDate
    val filePath: String get() = storagePath
    val remoteUrl: String? get() = photoUrl
    val syncStatus: String get() = if (photoUrl != null) "synced" else "local_only"
    val isVideo: Boolean get() = mediaType == MEDIA_TYPE_VIDEO
    val isPhoto: Boolean get() = mediaType != MEDIA_TYPE_VIDEO

    val displayMediaModel: Any
        get() {
            val file = java.io.File(storagePath)
            return if (file.exists()) file else (photoUrl ?: storagePath)
        }

    companion object {
        const val MEDIA_TYPE_PHOTO = "photo"
        const val MEDIA_TYPE_VIDEO = "video"
    }
}
