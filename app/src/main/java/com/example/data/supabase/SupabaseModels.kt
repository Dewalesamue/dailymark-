package com.example.data.supabase

import com.example.data.model.DailyPhoto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseAuthUser(
    val id: String,
    val email: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null,
    @Json(name = "created_at") val createdAt: String? = null
) {
    val displayName: String
        get() = (userMetadata?.get("full_name") as? String)
            ?: (userMetadata?.get("name") as? String)
            ?: email?.substringBefore("@")
            ?: "User"

    val avatarUrl: String?
        get() = (userMetadata?.get("avatar_url") as? String)
            ?: (userMetadata?.get("picture") as? String)
}

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    val user: SupabaseAuthUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseRemotePhoto(
    val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "storage_path") val storagePath: String,
    @Json(name = "photo_url") val photoUrl: String? = null,
    @Json(name = "journal_date") val journalDate: String,
    @Json(name = "captured_at") val capturedAt: Long = System.currentTimeMillis(),
    val caption: String? = null,
    val mood: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @Json(name = "media_type") val mediaType: String = "photo",
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @Json(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDailyPhoto(): DailyPhoto {
        return DailyPhoto(
            id = id,
            userId = userId,
            storagePath = storagePath,
            photoUrl = photoUrl,
            journalDate = journalDate,
            capturedAt = capturedAt,
            caption = caption,
            mood = mood,
            width = width,
            height = height,
            mediaType = mediaType,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDailyPhoto(photo: DailyPhoto): SupabaseRemotePhoto {
            return SupabaseRemotePhoto(
                id = photo.id,
                userId = photo.userId,
                storagePath = photo.storagePath,
                photoUrl = photo.photoUrl,
                journalDate = photo.journalDate,
                capturedAt = photo.capturedAt,
                caption = photo.caption,
                mood = photo.mood,
                width = photo.width,
                height = photo.height,
                mediaType = photo.mediaType,
                createdAt = photo.createdAt,
                updatedAt = photo.updatedAt
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class SupabaseProfile(
    val id: String,
    val email: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "updated_at") val updatedAt: Long? = System.currentTimeMillis()
)
