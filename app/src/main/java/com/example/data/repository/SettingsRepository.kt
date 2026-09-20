package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId

data class UserSettings(
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val profilePictureUri: String? = null,
    val accessToken: String? = null,
    val onboardingCompleted: Boolean = false,
    val themeMode: String = "system", // "system", "light", "dark"
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20, // 8:00 PM
    val reminderMinute: Int = 0,
    val hapticsEnabled: Boolean = true,
    val configuredTimeZone: String = ZoneId.systemDefault().id,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val isSignedIn: Boolean = false
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("one_photo_day_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val defaultTz = try {
            ZoneId.systemDefault().id
        } catch (_: Exception) {
            "UTC"
        }

        val savedUserId = prefs.getString("user_id", "") ?: ""
        val savedEmail = prefs.getString("user_email", "") ?: ""
        val savedName = prefs.getString("user_name", "") ?: ""
        val hasSession = prefs.getBoolean("is_signed_in", false) && savedUserId.isNotBlank()

        return UserSettings(
            userId = if (hasSession) savedUserId else "",
            userName = if (hasSession) savedName else "",
            userEmail = if (hasSession) savedEmail else "",
            profilePictureUri = prefs.getString("profile_picture_uri", null),
            accessToken = prefs.getString("access_token", null),
            onboardingCompleted = prefs.getBoolean("onboarding_completed", false),
            themeMode = prefs.getString("theme_mode", "system") ?: "system",
            reminderEnabled = prefs.getBoolean("reminder_enabled", true),
            reminderHour = prefs.getInt("reminder_hour", 20),
            reminderMinute = prefs.getInt("reminder_minute", 0),
            hapticsEnabled = prefs.getBoolean("haptics_enabled", true),
            configuredTimeZone = prefs.getString("configured_timezone", defaultTz) ?: defaultTz,
            supabaseUrl = prefs.getString("supabase_url", "") ?: "",
            supabaseAnonKey = prefs.getString("supabase_anon_key", "") ?: "",
            isSignedIn = hasSession
        )
    }

    fun setProfilePictureUri(uri: String?) {
        if (uri == null) {
            prefs.edit().remove("profile_picture_uri").apply()
        } else {
            prefs.edit().putString("profile_picture_uri", uri).apply()
        }
        _settings.value = _settings.value.copy(profilePictureUri = uri)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        _settings.value = _settings.value.copy(onboardingCompleted = completed)
    }

    fun setUserName(name: String) {
        val trimmed = name.trim()
        prefs.edit().putString("user_name", trimmed).apply()
        _settings.value = _settings.value.copy(userName = trimmed)
    }

    fun setUserEmail(email: String) {
        val trimmed = email.trim()
        prefs.edit().putString("user_email", trimmed).apply()
        _settings.value = _settings.value.copy(userEmail = trimmed)
    }

    fun signOut() {
        prefs.edit()
            .putBoolean("is_signed_in", false)
            .remove("user_id")
            .remove("user_email")
            .remove("user_name")
            .remove("profile_picture_uri")
            .remove("access_token")
            .apply()
        _settings.value = _settings.value.copy(
            isSignedIn = false,
            userId = "",
            userEmail = "",
            userName = "",
            profilePictureUri = null,
            accessToken = null
        )
    }

    fun signInUser(userId: String, email: String, name: String, avatarUrl: String? = null, token: String? = null) {
        val trimmedUserId = userId.trim()
        val trimmedEmail = email.trim()
        val trimmedName = name.trim().ifEmpty { trimmedEmail.substringBefore("@") }
        val editor = prefs.edit()
            .putBoolean("is_signed_in", true)
            .putString("user_id", trimmedUserId)
            .putString("user_email", trimmedEmail)
            .putString("user_name", trimmedName)

        if (avatarUrl != null) {
            editor.putString("profile_picture_uri", avatarUrl)
        }
        if (token != null) {
            editor.putString("access_token", token)
        }
        editor.apply()

        _settings.value = _settings.value.copy(
            isSignedIn = true,
            userId = trimmedUserId,
            userEmail = trimmedEmail,
            userName = trimmedName,
            profilePictureUri = avatarUrl ?: _settings.value.profilePictureUri,
            accessToken = token ?: _settings.value.accessToken
        )
    }

    fun signIn(email: String = "adelekesam10@gmail.com", name: String = "Sam Adeleke") {
        val deterministicUid = "user_" + java.util.UUID.nameUUIDFromBytes(email.lowercase().toByteArray()).toString()
        signInUser(deterministicUid, email, name)
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reminder_enabled", enabled).apply()
        _settings.value = _settings.value.copy(reminderEnabled = enabled)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()
        _settings.value = _settings.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
        _settings.value = _settings.value.copy(hapticsEnabled = enabled)
    }

    fun setConfiguredTimeZone(timeZoneId: String) {
        val trimmed = timeZoneId.trim().ifEmpty { ZoneId.systemDefault().id }
        prefs.edit().putString("configured_timezone", trimmed).apply()
        _settings.value = _settings.value.copy(configuredTimeZone = trimmed)
    }

    fun setSupabaseConfig(url: String, anonKey: String) {
        prefs.edit()
            .putString("supabase_url", url.trim())
            .putString("supabase_anon_key", anonKey.trim())
            .apply()
        _settings.value = _settings.value.copy(supabaseUrl = url.trim(), supabaseAnonKey = anonKey.trim())
    }

    private val _dayCustomTitles = MutableStateFlow<Map<String, String>>(loadDayCustomTitles())
    val dayCustomTitles: StateFlow<Map<String, String>> = _dayCustomTitles.asStateFlow()

    private fun loadDayCustomTitles(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        prefs.all.forEach { (k, v) ->
            if (k.startsWith("day_custom_title_") && v is String) {
                val date = k.removePrefix("day_custom_title_")
                if (v.isNotBlank()) {
                    map[date] = v
                }
            }
        }
        return map
    }

    fun setDayCustomTitle(date: String, title: String?) {
        val key = "day_custom_title_$date"
        val trimmed = title?.trim()?.ifEmpty { null }
        if (trimmed == null) {
            prefs.edit().remove(key).apply()
            _dayCustomTitles.value = _dayCustomTitles.value - date
        } else {
            prefs.edit().putString(key, trimmed).apply()
            _dayCustomTitles.value = _dayCustomTitles.value + (date to trimmed)
        }
    }

    fun getDayCustomTitle(date: String): String? {
        return _dayCustomTitles.value[date]
    }
}
