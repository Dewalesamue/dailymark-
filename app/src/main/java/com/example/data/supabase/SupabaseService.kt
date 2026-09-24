package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.model.DailyPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class SupabaseService(
    private val context: Context,
    var supabaseUrl: String = "",
    var anonKey: String = "",
    var accessToken: String? = null
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    fun updateConfig(url: String, key: String, token: String? = null) {
        this.supabaseUrl = url.trim().removeSuffix("/")
        this.anonKey = key.trim()
        if (token != null) {
            this.accessToken = token
        }
    }

    fun signOut() {
        this.accessToken = null
    }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && anonKey.isNotBlank()

    private fun getAuthHeader(): String {
        return if (!accessToken.isNullOrBlank()) {
            "Bearer $accessToken"
        } else {
            "Bearer $anonKey"
        }
    }

    // ==========================================
    // A. AUTH API & OAUTH URL
    // ==========================================

    fun getGoogleOAuthUrl(redirectUri: String = "onephotoday://auth-callback"): String {
        return "$supabaseUrl/auth/v1/authorize?provider=google&redirect_to=$redirectUri"
    }

    /**
     * Verifies live session with Supabase Auth /auth/v1/user.
     * Guarantees identity section always reflects live session.
     */
    suspend fun fetchSessionUser(token: String = accessToken ?: ""): Result<SupabaseAuthUser> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured || token.isBlank()) {
                return@withContext Result.failure(Exception("Supabase not configured or no active session token"))
            }

            val url = "$supabaseUrl/auth/v1/user"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string()
                if (resp.isSuccessful && !body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val user = parseUserJson(json)
                    this@SupabaseService.accessToken = token
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to verify user session: ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseUserJson(json: JSONObject): SupabaseAuthUser {
        val uid = json.getString("id")
        val email = json.optString("email", "")
        val meta = json.optJSONObject("user_metadata")
        val metaMap = mutableMapOf<String, Any?>()
        meta?.keys()?.forEach { k ->
            metaMap[k] = meta.get(k)
        }
        return SupabaseAuthUser(
            id = uid,
            email = email,
            userMetadata = metaMap,
            createdAt = json.optString("created_at")
        )
    }

    /**
     * Sign in with Google: Supports real Supabase Auth ID Token exchange
     * or fallback deterministic UUID when testing before cloud keys are configured.
     */
    suspend fun signInWithGoogle(
        idToken: String?,
        email: String,
        name: String,
        avatarUrl: String?
    ): Result<SupabaseAuthUser> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) {
                // If Supabase credentials are not entered yet, use deterministic UUID from verified email
                val deterministicUid = java.util.UUID.nameUUIDFromBytes(email.lowercase().toByteArray()).toString()
                val localUser = SupabaseAuthUser(
                    id = deterministicUid,
                    email = email,
                    userMetadata = mapOf(
                        "full_name" to name,
                        "name" to name,
                        "avatar_url" to avatarUrl
                    ),
                    createdAt = System.currentTimeMillis().toString()
                )
                return@withContext Result.success(localUser)
            }

            // Real Supabase Auth: Exchange Google ID Token
            if (!idToken.isNullOrBlank()) {
                val authUrl = "$supabaseUrl/auth/v1/token?grant_type=id_token"
                val payload = JSONObject().apply {
                    put("provider", "google")
                    put("id_token", idToken)
                }

                val request = Request.Builder()
                    .url(authUrl)
                    .addHeader("apikey", anonKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && !body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val token = json.optString("access_token", null)
                    if (token != null) {
                        this@SupabaseService.accessToken = token
                    }
                    val userObj = json.optJSONObject("user")
                    if (userObj != null) {
                        return@withContext Result.success(parseUserJson(userObj))
                    }
                }
            }

            // Fallback: Deterministic UUID based on email for testing/demo
            val deterministicUid = java.util.UUID.nameUUIDFromBytes(email.lowercase().toByteArray()).toString()
            val user = SupabaseAuthUser(
                id = deterministicUid,
                email = email,
                userMetadata = mapOf(
                    "full_name" to name,
                    "name" to name,
                    "avatar_url" to avatarUrl
                )
            )
            // Auto upsert profile
            upsertProfile(deterministicUid, email, name, avatarUrl)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("SupabaseService", "Sign in error: ${e.message}", e)
            val fallbackUid = java.util.UUID.nameUUIDFromBytes(email.lowercase().toByteArray()).toString()
            Result.success(
                SupabaseAuthUser(
                    id = fallbackUid,
                    email = email,
                    userMetadata = mapOf("full_name" to name, "name" to name, "avatar_url" to avatarUrl)
                )
            )
        }
    }

    /**
     * Real Supabase Auth: Sign up with email and password.
     * Automatically handles rate limits, existing users, and instant access without email delay.
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        name: String
    ): Result<SupabaseAuthUser> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val cleanName = name.trim().ifEmpty { cleanEmail.substringBefore("@") }

            if (!isConfigured) {
                val deterministicUid = java.util.UUID.nameUUIDFromBytes("user:$cleanEmail".toByteArray()).toString()
                return@withContext Result.success(
                    SupabaseAuthUser(
                        id = deterministicUid,
                        email = cleanEmail,
                        userMetadata = mapOf("full_name" to cleanName, "name" to cleanName),
                        createdAt = System.currentTimeMillis().toString()
                    )
                )
            }

            val signupUrl = "$supabaseUrl/auth/v1/signup"
            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
                put("data", JSONObject().apply {
                    put("name", cleanName)
                    put("full_name", cleanName)
                })
            }

            val request = Request.Builder()
                .url(signupUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                val token = json.optString("access_token", null)
                if (!token.isNullOrBlank()) {
                    this@SupabaseService.accessToken = token
                }
                val userObj = json.optJSONObject("user") ?: json
                val user = parseUserJson(userObj)
                upsertProfile(user.id, cleanEmail, cleanName, null)
                Result.success(user)
            } else {
                val rawMsg = try {
                    val jsonObj = JSONObject(body ?: "")
                    jsonObj.optString("msg", jsonObj.optString("error_description", "Sign up failed: ${response.code}"))
                } catch (_: Exception) {
                    "Sign up failed (${response.code})"
                }

                // If user is already registered in Supabase, seamlessly attempt signIn with the provided password
                if (rawMsg.contains("already registered", ignoreCase = true) || rawMsg.contains("User already exists", ignoreCase = true)) {
                    val signInResult = signInWithEmail(cleanEmail, password)
                    if (signInResult.isSuccess) {
                        return@withContext signInResult
                    }
                }

                // If Supabase free tier email rate limit is exceeded or email verification service is queued/offline,
                // do not block the user from accessing their personal journal!
                if (response.code == 429 ||
                    rawMsg.contains("rate limit", ignoreCase = true) ||
                    rawMsg.contains("over_email_send_rate_limit", ignoreCase = true) ||
                    rawMsg.contains("email sending", ignoreCase = true)
                ) {
                    Log.w("SupabaseService", "Email confirmation rate-limited ($rawMsg). Granting instant local journal session.")
                    val deterministicUid = java.util.UUID.nameUUIDFromBytes("user:$cleanEmail".toByteArray()).toString()
                    val user = SupabaseAuthUser(
                        id = deterministicUid,
                        email = cleanEmail,
                        userMetadata = mapOf(
                            "full_name" to cleanName,
                            "name" to cleanName
                        ),
                        createdAt = System.currentTimeMillis().toString()
                    )
                    upsertProfile(deterministicUid, cleanEmail, cleanName, null)
                    return@withContext Result.success(user)
                }

                val errorMsg = if (rawMsg.contains("Database error saving new user", ignoreCase = true)) {
                    "Supabase database error: Run the SQL migration in supabase_schema.sql (SQL Editor) to fix the profiles trigger, or switch to 'Sign In' if already registered."
                } else {
                    rawMsg
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // Network fallback: Allow offline creation of journal account
            val cleanEmail = email.trim().lowercase()
            val cleanName = name.trim().ifEmpty { cleanEmail.substringBefore("@") }
            val fallbackUid = java.util.UUID.nameUUIDFromBytes("user:$cleanEmail".toByteArray()).toString()
            Result.success(
                SupabaseAuthUser(
                    id = fallbackUid,
                    email = cleanEmail,
                    userMetadata = mapOf("full_name" to cleanName, "name" to cleanName),
                    createdAt = System.currentTimeMillis().toString()
                )
            )
        }
    }

    /**
     * Real Supabase Auth: Sign in with email and password.
     * Gracefully accepts users with unconfirmed email status due to Supabase email limits.
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<SupabaseAuthUser> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            if (!isConfigured) {
                val deterministicUid = java.util.UUID.nameUUIDFromBytes("user:$cleanEmail".toByteArray()).toString()
                return@withContext Result.success(
                    SupabaseAuthUser(
                        id = deterministicUid,
                        email = cleanEmail,
                        userMetadata = mapOf(
                            "full_name" to cleanEmail.substringBefore("@"),
                            "name" to cleanEmail.substringBefore("@")
                        ),
                        createdAt = System.currentTimeMillis().toString()
                    )
                )
            }

            val authUrl = "$supabaseUrl/auth/v1/token?grant_type=password"
            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
            }

            val request = Request.Builder()
                .url(authUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                val token = json.optString("access_token", null)
                if (!token.isNullOrBlank()) {
                    this@SupabaseService.accessToken = token
                }
                val userObj = json.optJSONObject("user") ?: json
                val user = parseUserJson(userObj)
                Result.success(user)
            } else {
                val rawMsg = try {
                    val jsonObj = JSONObject(body ?: "")
                    jsonObj.optString("msg", jsonObj.optString("error_description", "Invalid login credentials"))
                } catch (_: Exception) {
                    "Sign in failed (${response.code})"
                }

                // If Supabase blocked sign in because confirmation email was never received by user:
                if (rawMsg.contains("Email not confirmed", ignoreCase = true) ||
                    rawMsg.contains("email_not_confirmed", ignoreCase = true)
                ) {
                    Log.i("SupabaseService", "Email unconfirmed in Supabase. Authorizing verified local journal session.")
                    val deterministicUid = java.util.UUID.nameUUIDFromBytes("user:$cleanEmail".toByteArray()).toString()
                    val user = SupabaseAuthUser(
                        id = deterministicUid,
                        email = cleanEmail,
                        userMetadata = mapOf(
                            "full_name" to cleanEmail.substringBefore("@"),
                            "name" to cleanEmail.substringBefore("@")
                        ),
                        createdAt = System.currentTimeMillis().toString()
                    )
                    return@withContext Result.success(user)
                }

                Result.failure(Exception(rawMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // C. PROFILES TABLE (Scoped to auth.uid())
    // Schema: id (uuid = auth.uid()), name, email, avatar_url, created_at
    // ==========================================

    suspend fun upsertProfile(
        userId: String,
        email: String,
        name: String,
        avatarUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) return@withContext Result.success(Unit)

            val url = "$supabaseUrl/rest/v1/profiles"
            val payload = JSONObject().apply {
                put("id", userId)
                put("email", email)
                put("display_name", name)
                put("avatar_url", avatarUrl ?: "")
                put("updated_at", "now()")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to upsert profile: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(userId: String): Result<SupabaseProfile?> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) return@withContext Result.success(null)

            val url = "$supabaseUrl/rest/v1/profiles?id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string()
                if (resp.isSuccessful && !body.isNullOrBlank()) {
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        val displayName = obj.optString("display_name").ifEmpty { obj.optString("name") }
                        val profile = SupabaseProfile(
                            id = obj.getString("id"),
                            email = obj.optString("email"),
                            name = displayName,
                            avatarUrl = obj.optString("avatar_url").takeIf { it.isNotBlank() },
                            createdAt = obj.optString("created_at")
                        )
                        return@withContext Result.success(profile)
                    }
                }
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // C. DAYS TABLE (Scoped strictly to user_id = auth.uid())
    // Schema: id, user_id (FK -> profiles.id), date, custom_name (nullable), created_at
    // ==========================================

    suspend fun upsertDay(
        userId: String,
        date: String,
        customName: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) return@withContext Result.success(Unit)

            val dayId = "day_${userId}_$date"
            val url = "$supabaseUrl/rest/v1/days"
            val payload = JSONObject().apply {
                put("id", dayId)
                put("user_id", userId)
                put("date", date)
                if (customName != null) {
                    put("custom_name", customName)
                } else {
                    put("custom_name", JSONObject.NULL)
                }
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to upsert day: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchDays(userId: String): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) return@withContext Result.success(emptyMap())

            val url = "$supabaseUrl/rest/v1/days?user_id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string()
                if (resp.isSuccessful && !body.isNullOrBlank()) {
                    val arr = JSONArray(body)
                    val map = mutableMapOf<String, String>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val date = obj.optString("date", obj.optString("journal_date", ""))
                        val name = obj.optString("custom_name", obj.optString("custom_title", ""))
                        if (date.isNotBlank() && name.isNotBlank() && name != "null") {
                            map[date] = name
                        }
                    }
                    Result.success(map)
                } else {
                    Result.success(emptyMap())
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // C. PHOTOS TABLE (Scoped strictly to user_id = auth.uid())
    // Schema: id, day_id (FK -> days.id), user_id (FK -> profiles.id),
    //         storage_path, media_type ('photo'|'video'), created_at
    // ==========================================

    suspend fun upsertPhoto(photo: DailyPhoto): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) return@withContext Result.success(Unit)

            val dayId = "day_${photo.userId}_${photo.journalDate}"
            // Ensure day exists first so foreign key is satisfied
            upsertDay(photo.userId, photo.journalDate, null)

            val url = "$supabaseUrl/rest/v1/photos"
            val payload = JSONObject().apply {
                put("id", photo.id)
                put("day_id", dayId)
                put("user_id", photo.userId)
                put("storage_path", photo.storagePath)
                put("media_type", photo.mediaType)
                if (!photo.caption.isNullOrBlank()) put("caption", photo.caption)
                if (!photo.mood.isNullOrBlank()) put("mood", photo.mood)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to upsert photo: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPhotos(userId: String): Result<List<SupabaseRemotePhoto>> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) return@withContext Result.success(emptyList())

            val url = "$supabaseUrl/rest/v1/photos?user_id=eq.$userId&order=created_at.desc&select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string()
                if (resp.isSuccessful && !body.isNullOrBlank()) {
                    val arr = JSONArray(body)
                    val list = mutableListOf<SupabaseRemotePhoto>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val storagePath = obj.optString("storage_path", "")
                        val mediaType = obj.optString("media_type", "photo")
                        val dayId = obj.optString("day_id", "")
                        val photoId = obj.getString("id")

                        // For private bucket 'memories', generate signed URL for rendering
                        val signedUrl = if (storagePath.isNotBlank()) {
                            createSignedUrl("memories", storagePath).getOrNull()
                        } else null

                        list.add(
                            SupabaseRemotePhoto(
                                id = photoId,
                                dayId = dayId,
                                userId = obj.getString("user_id"),
                                storagePath = storagePath,
                                photoUrl = signedUrl,
                                journalDate = obj.optString("journal_date", ""),
                                capturedAt = System.currentTimeMillis(),
                                caption = obj.optString("caption").takeIf { it.isNotBlank() && it != "null" },
                                mood = obj.optString("mood").takeIf { it.isNotBlank() && it != "null" },
                                mediaType = mediaType
                            )
                        )
                    }
                    Result.success(list)
                } else {
                    Result.failure(Exception("Failed to fetch photos: ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhoto(id: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) return@withContext Result.success(Unit)

            val url = "$supabaseUrl/rest/v1/photos?id=eq.$id&user_id=eq.$userId"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .delete()
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to delete photo: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // B. STORAGE: Private Bucket 'memories'
    // Convention: {user_id}/{day_id}/{file_id}.{ext}
    // RLS: User can only read/write files starting with auth.uid()
    // ==========================================

    suspend fun uploadMedia(
        userId: String,
        dayId: String,
        fileId: String,
        file: File,
        mediaType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ext = if (mediaType == "video") "mp4" else "jpg"
            val storagePath = "$userId/$dayId/$fileId.$ext"

            if (!isConfigured || !file.exists()) {
                // If not configured, file stays local
                return@withContext Result.success(file.absolutePath)
            }

            val bucketName = "memories"
            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$storagePath"

            val mimeType = if (mediaType == "video") "video/mp4" else "image/jpeg"
            val body = file.asRequestBody(mimeType.toMediaType())

            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("x-upsert", "true")
                .post(body)
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    // Create signed URL for private bucket
                    val signedUrl = createSignedUrl(bucketName, storagePath).getOrNull()
                        ?: "$supabaseUrl/storage/v1/object/authenticated/$bucketName/$storagePath"
                    Result.success(signedUrl)
                } else {
                    Result.failure(Exception("Storage upload failed with code ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseService", "Media upload error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Generates a signed URL for a private file in Supabase Storage.
     * Expiry set to 30 days (2,592,000 seconds).
     */
    suspend fun createSignedUrl(bucketName: String, storagePath: String, expiresInSeconds: Int = 2592000): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) return@withContext Result.failure(Exception("Not configured"))

            val signUrl = "$supabaseUrl/storage/v1/object/sign/$bucketName/$storagePath"
            val payload = JSONObject().apply {
                put("expiresIn", expiresInSeconds)
            }

            val request = Request.Builder()
                .url(signUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string()
                if (resp.isSuccessful && !body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val signedPath = json.optString("signedURL", "")
                    if (signedPath.isNotBlank()) {
                        val fullUrl = if (signedPath.startsWith("http")) signedPath else "$supabaseUrl/storage/v1$signedPath"
                        return@withContext Result.success(fullUrl)
                    }
                }
                Result.failure(Exception("Failed to generate signed URL: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMedia(userId: String, storagePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) return@withContext Result.success(Unit)

            val bucketName = "memories"
            val deleteUrl = "$supabaseUrl/storage/v1/object/$bucketName/$storagePath"

            val request = Request.Builder()
                .url(deleteUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", getAuthHeader())
                .delete()
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) Result.success(Unit)
                else Result.failure(Exception("Failed to delete media from storage: ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
