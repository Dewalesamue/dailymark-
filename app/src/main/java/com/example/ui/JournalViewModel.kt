package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DailyPhoto
import com.example.data.repository.JournalStats
import com.example.data.repository.PhotoRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserSettings
import com.example.data.supabase.SupabaseService
import com.example.util.DateTimeUtils
import com.example.util.ExifHelper
import com.example.util.HapticUtils
import com.example.util.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

sealed class Screen {
    data object MainTabs : Screen()
    data object Onboarding : Screen()
    data object CameraCapture : Screen() // Camera is strictly for today's moment
    data class PhotoConfirm(
        val bitmap: Bitmap? = null,
        val uri: Uri? = null,
        val targetDate: LocalDate,
        val isFromCamera: Boolean = false,
        val isVideo: Boolean = false,
        val suggestedExifDate: LocalDate? = null,
        val currentDayCount: Int = 0
    ) : Screen()
    data class PhotoDetail(
        val targetDate: String,
        val initialPhotoId: String? = null
    ) : Screen()
    data object Notifications : Screen()
}

enum class NavigationTab {
    HOME,
    CALENDAR,
    MEMORIES,
    PROFILE
}

data class SavingProgress(val current: Int, val total: Int)

data class JournalUiState(
    val photos: List<DailyPhoto> = emptyList(),
    val todayPhotos: List<DailyPhoto> = emptyList(),
    val todayPhoto: DailyPhoto? = null,
    val stats: JournalStats = JournalStats(),
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDatePhotos: List<DailyPhoto> = emptyList(),
    val selectedDatePhoto: DailyPhoto? = null,
    val currentTab: NavigationTab = NavigationTab.HOME,
    val currentScreen: Screen = Screen.MainTabs,
    val isSaving: Boolean = false,
    val savingProgress: SavingProgress? = null,
    val saveMessage: String? = null,
    val activeDateActionSheet: LocalDate? = null,
    val configuredTimeZone: String = ZoneId.systemDefault().id,
    val dayCustomTitles: Map<String, String> = emptyMap()
)

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val settingsRepository = SettingsRepository(application)
    val settings: StateFlow<UserSettings> = settingsRepository.settings

    val supabaseService = SupabaseService(
        context = application,
        supabaseUrl = settingsRepository.settings.value.supabaseUrl,
        anonKey = settingsRepository.settings.value.supabaseAnonKey,
        accessToken = settingsRepository.settings.value.accessToken
    )
    private val photoRepository = PhotoRepository(application, database.photoDao(), supabaseService)

    @OptIn(ExperimentalCoroutinesApi::class)
    val userPhotosFlow: Flow<List<DailyPhoto>> = settings
        .map { it.userId }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            photoRepository.getAllPhotos(userId)
        }

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.MainTabs)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _savingProgress = MutableStateFlow<SavingProgress?>(null)
    val savingProgress: StateFlow<SavingProgress?> = _savingProgress.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    private val _activeDateActionSheet = MutableStateFlow<LocalDate?>(null)
    val activeDateActionSheet: StateFlow<LocalDate?> = _activeDateActionSheet.asStateFlow()

    init {
        val today = getTodayDate()
        _selectedDate.value = today
        _currentMonth.value = YearMonth.from(today)

        if (!settings.value.onboardingCompleted) {
            _currentScreen.value = Screen.Onboarding
        }
        ReminderManager.createNotificationChannel(application)

        // Keep SupabaseService reactive to settings changes
        viewModelScope.launch {
            settings.collect { s ->
                supabaseService.updateConfig(s.supabaseUrl, s.supabaseAnonKey, s.accessToken)
            }
        }

        // Whenever user is signed in, sync cloud memories automatically
        viewModelScope.launch {
            settings.map { it.userId to it.isSignedIn }.distinctUntilChanged().collect { (uid, signedIn) ->
                if (signedIn && uid != "local_user") {
                    photoRepository.syncFromCloud(uid)
                    val daysResult = supabaseService.fetchDays(uid)
                    if (daysResult.isSuccess) {
                        daysResult.getOrNull()?.forEach { (date, title) ->
                            settingsRepository.setDayCustomTitle(date, title)
                        }
                    }
                }
            }
        }
    }

    fun getTodayDate(): LocalDate {
        return DateTimeUtils.getToday(settings.value.configuredTimeZone)
    }

    val uiState: StateFlow<JournalUiState> = combine(
        userPhotosFlow,
        _currentMonth,
        _selectedDate,
        _currentTab,
        _currentScreen,
        _activeDateActionSheet,
        settings,
        settingsRepository.dayCustomTitles
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val photos = args[0] as List<DailyPhoto>
        val month = args[1] as YearMonth
        val selDate = args[2] as LocalDate
        val tab = args[3] as NavigationTab
        val screen = args[4] as Screen
        val actionSheetDate = args[5] as LocalDate?
        val userSettings = args[6] as UserSettings
        val dayCustomTitles = args[7] as Map<String, String>

        val today = DateTimeUtils.getToday(userSettings.configuredTimeZone)
        val todayStr = DateTimeUtils.toIsoDate(today)
        val todayPhotos = photos.filter { it.journalDate == todayStr }.sortedByDescending { it.capturedAt }
        val selDateStr = DateTimeUtils.toIsoDate(selDate)
        val selectedPhotos = photos.filter { it.journalDate == selDateStr }.sortedByDescending { it.capturedAt }
        val stats = PhotoRepository.calculateStats(photos, today)

        JournalUiState(
            photos = photos,
            todayPhotos = todayPhotos,
            todayPhoto = todayPhotos.firstOrNull(),
            stats = stats,
            currentMonth = month,
            selectedDate = selDate,
            selectedDatePhotos = selectedPhotos,
            selectedDatePhoto = selectedPhotos.firstOrNull(),
            currentTab = tab,
            currentScreen = screen,
            isSaving = _isSaving.value,
            savingProgress = _savingProgress.value,
            saveMessage = _saveMessage.value,
            activeDateActionSheet = actionSheetDate,
            configuredTimeZone = userSettings.configuredTimeZone,
            dayCustomTitles = dayCustomTitles
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JournalUiState()
    )

    fun setDayCustomTitle(date: String, title: String?) {
        settingsRepository.setDayCustomTitle(date, title)
        HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 20)
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
        _currentScreen.value = Screen.MainTabs
        _activeDateActionSheet.value = null
        HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 15)
    }

    fun openScreen(screen: Screen) {
        _currentScreen.value = screen
        _activeDateActionSheet.value = null
        HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 20)
    }

    fun closeToTabs() {
        _currentScreen.value = Screen.MainTabs
        _activeDateActionSheet.value = null
    }

    fun openNotifications() {
        openScreen(Screen.Notifications)
    }

    /**
     * Camera Rule: The camera always represents the current moment.
     * Always uses today's local date. Never provides a date picker inside camera.
     */
    fun openCameraForToday() {
        _activeDateActionSheet.value = null
        _currentScreen.value = Screen.CameraCapture
        HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 25)
    }

    /**
     * Backward-compatible alias for camera capture.
     * Rejects any date that is not today.
     */
    fun openCameraForDate(date: LocalDate = getTodayDate()) {
        val today = getTodayDate()
        if (date.isAfter(today)) {
            _saveMessage.value = "That day hasn't happened yet. Come back when the day arrives."
            return
        }
        if (date.isBefore(today)) {
            _saveMessage.value = "Camera is only for today. You can add existing photos to past days."
            return
        }
        openCameraForToday()
    }

    /**
     * Invoked after user snaps a photo with camera.
     * Target date is strictly locked to today.
     */
    fun onCameraPhotoCaptured(bitmap: Bitmap) {
        val today = getTodayDate()
        val todayStr = DateTimeUtils.toIsoDate(today)
        val uid = settings.value.userId
        viewModelScope.launch {
            val count = photoRepository.getPhotoCountForDate(uid, todayStr)
            _currentScreen.value = Screen.PhotoConfirm(
                bitmap = bitmap,
                targetDate = today,
                isFromCamera = true,
                isVideo = false,
                suggestedExifDate = null,
                currentDayCount = count
            )
        }
    }

    /**
     * Invoked after user records a video clip with camera.
     * Target date is strictly locked to today.
     */
    fun onCameraVideoCaptured(uri: Uri) {
        val today = getTodayDate()
        val todayStr = DateTimeUtils.toIsoDate(today)
        val uid = settings.value.userId
        viewModelScope.launch {
            val count = photoRepository.getPhotoCountForDate(uid, todayStr)
            _currentScreen.value = Screen.PhotoConfirm(
                uri = uri,
                targetDate = today,
                isFromCamera = true,
                isVideo = true,
                suggestedExifDate = null,
                currentDayCount = count
            )
        }
    }

    /**
     * Gallery Upload Rule:
     * Allowed: Today or Any past date.
     * Forbidden: Tomorrow or Any future date.
     */
    fun handleGalleryPick(uris: List<Uri>, targetDate: LocalDate) {
        val today = getTodayDate()
        if (targetDate.isAfter(today)) {
            _saveMessage.value = "That day hasn't happened yet. Come back when the day arrives."
            return
        }
        if (uris.isEmpty()) return
        _activeDateActionSheet.value = null

        val uid = settings.value.userId
        viewModelScope.launch {
            val dateStr = DateTimeUtils.toIsoDate(targetDate)
            val existingCount = photoRepository.getPhotoCountForDate(uid, dateStr)

            if (uris.size == 1) {
                val singleUri = uris.first()
                val mimeType = getApplication<Application>().contentResolver.getType(singleUri) ?: ""
                val isVideo = mimeType.startsWith("video/")

                val exif = if (!isVideo) ExifHelper.extractExifData(getApplication(), singleUri, today) else null
                val suggestedDate = if (exif?.suggestedDate != null && exif.suggestedDate != targetDate) {
                    exif.suggestedDate
                } else null

                _currentScreen.value = Screen.PhotoConfirm(
                    uri = singleUri,
                    targetDate = targetDate,
                    isFromCamera = false,
                    isVideo = isVideo,
                    suggestedExifDate = suggestedDate,
                    currentDayCount = existingCount
                )
            } else {
                // Multiple media selected at once - save memories
                saveMultiplePhotos(uris = uris, targetDate = targetDate)
            }
        }
    }

    fun saveMultiplePhotos(uris: List<Uri>, targetDate: LocalDate) {
        val today = getTodayDate()
        if (targetDate.isAfter(today)) {
            _saveMessage.value = "That day hasn't happened yet. Come back when the day arrives."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _savingProgress.value = SavingProgress(0, uris.size)
            val result = photoRepository.saveMultiplePhotos(
                uris = uris,
                journalDate = targetDate,
                userId = settings.value.userId,
                userTimeZone = settings.value.configuredTimeZone,
                onProgress = { current, total ->
                    _savingProgress.value = SavingProgress(current, total)
                }
            )
            _isSaving.value = false
            _savingProgress.value = null

            if (result.isSuccess) {
                val savedList = result.getOrNull() ?: emptyList()
                val dateLabel = if (targetDate == today) "today" else DateTimeUtils.formatShortDate(targetDate)
                HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 45)
                _saveMessage.value = "Added ${savedList.size} memories to $dateLabel"
                _currentScreen.value = Screen.MainTabs
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unable to save photos"
                _saveMessage.value = errorMsg
            }
        }
    }

    fun saveConfirmedPhoto(
        bitmap: Bitmap?,
        uri: Uri?,
        targetDate: LocalDate,
        caption: String?,
        mood: String?,
        onSuccess: () -> Unit
    ) {
        val today = getTodayDate()
        if (targetDate.isAfter(today)) {
            _saveMessage.value = "That day hasn't happened yet. Come back when the day arrives."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val result = photoRepository.savePhoto(
                bitmap = bitmap,
                uri = uri,
                journalDate = targetDate,
                userId = settings.value.userId,
                caption = caption,
                mood = mood,
                userTimeZone = settings.value.configuredTimeZone
            )
            _isSaving.value = false
            if (result.isSuccess) {
                HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 45)
                _saveMessage.value = if (targetDate == today) "Today's moment captured!" else "Memory saved!"
                _currentScreen.value = Screen.MainTabs
                onSuccess()
            } else {
                _saveMessage.value = result.exceptionOrNull()?.message ?: "Unable to save photo"
            }
        }
    }

    fun saveVideo(
        uri: Uri,
        targetDate: LocalDate,
        caption: String?,
        mood: String?,
        onSuccess: () -> Unit = {}
    ) {
        val today = getTodayDate()
        if (targetDate.isAfter(today)) {
            _saveMessage.value = "That day hasn't happened yet. Come back when the day arrives."
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val result = photoRepository.saveVideo(
                uri = uri,
                journalDate = targetDate,
                userId = settings.value.userId,
                caption = caption,
                mood = mood,
                userTimeZone = settings.value.configuredTimeZone
            )
            _isSaving.value = false
            if (result.isSuccess) {
                HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 45)
                _saveMessage.value = if (targetDate == today) "Today's video clip captured!" else "Video memory saved!"
                _currentScreen.value = Screen.MainTabs
                onSuccess()
            } else {
                _saveMessage.value = result.exceptionOrNull()?.message ?: "Unable to save video"
            }
        }
    }

    fun openDateActionSheet(date: LocalDate) {
        val today = getTodayDate()
        _selectedDate.value = date
        if (date.isAfter(today)) {
            // Future date tapped: visually communicate and show human message
            _saveMessage.value = "That day hasn't happened yet. Come back when the day arrives."
            _activeDateActionSheet.value = date // To display the future date sheet
        } else {
            _activeDateActionSheet.value = date
        }
        HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 20)
    }

    fun dismissDateActionSheet() {
        _activeDateActionSheet.value = null
    }

    fun openDetailForDate(dateStr: String, photoId: String? = null) {
        _activeDateActionSheet.value = null
        _currentScreen.value = Screen.PhotoDetail(targetDate = dateStr, initialPhotoId = photoId)
        HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 20)
    }

    fun updateCaptionAndMood(photo: DailyPhoto, newCaption: String?, newMood: String?) {
        viewModelScope.launch {
            val updated = photo.copy(
                caption = newCaption?.trim()?.ifEmpty { null },
                mood = newMood?.trim()?.ifEmpty { null }
            )
            photoRepository.updatePhoto(updated)
            HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 20)
        }
    }

    fun deletePhoto(photo: DailyPhoto, onDeleted: () -> Unit = {}) {
        val uid = settings.value.userId
        viewModelScope.launch {
            photoRepository.deletePhoto(photo, uid)
            HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 30)
            onDeleted()
        }
    }

    fun navigateMonth(deltaMonths: Long) {
        _currentMonth.value = _currentMonth.value.plusMonths(deltaMonths)
        HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 15)
    }

    fun jumpToToday() {
        val today = getTodayDate()
        _currentMonth.value = YearMonth.from(today)
        _selectedDate.value = today
        HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 20)
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        openDateActionSheet(date)
    }

    fun completeOnboarding() {
        settingsRepository.setOnboardingCompleted(true)
        _currentScreen.value = Screen.MainTabs
        HapticUtils.performHaptic(getApplication(), true, 30)
    }

    fun resetOnboarding() {
        settingsRepository.setOnboardingCompleted(false)
        _currentScreen.value = Screen.Onboarding
    }

    fun updateUserName(name: String) {
        settingsRepository.setUserName(name)
        val uid = settings.value.userId
        if (settings.value.isSignedIn && uid != "local_user") {
            viewModelScope.launch {
                supabaseService.upsertProfile(uid, settings.value.userEmail, name, settings.value.profilePictureUri)
            }
        }
    }

    fun updateUserEmail(email: String) {
        settingsRepository.setUserEmail(email)
        val uid = settings.value.userId
        if (settings.value.isSignedIn && uid != "local_user") {
            viewModelScope.launch {
                supabaseService.upsertProfile(uid, email, settings.value.userName, settings.value.profilePictureUri)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            supabaseService.signOut()
            settingsRepository.signOut()
            HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 25)
            _saveMessage.value = "Signed out"
        }
    }

    fun signInWithGoogle(
        email: String,
        name: String,
        avatarUrl: String? = null,
        idToken: String? = null,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            _saveMessage.value = "Authenticating with Supabase..."
            val result = supabaseService.signInWithGoogle(
                idToken = idToken,
                email = email,
                name = name,
                avatarUrl = avatarUrl
            )

            if (result.isSuccess) {
                val authUser = result.getOrThrow()
                settingsRepository.signInUser(
                    userId = authUser.id,
                    email = authUser.email ?: email,
                    name = authUser.displayName,
                    avatarUrl = authUser.avatarUrl ?: avatarUrl,
                    token = supabaseService.accessToken
                )

                // Sync cloud memories for this authenticated user
                _saveMessage.value = "Restoring cloud memories..."
                photoRepository.syncFromCloud(authUser.id)

                // Sync custom day titles
                val daysResult = supabaseService.fetchDays(authUser.id)
                if (daysResult.isSuccess) {
                    daysResult.getOrNull()?.forEach { (date, title) ->
                        settingsRepository.setDayCustomTitle(date, title)
                    }
                }

                _saveMessage.value = "Signed in as ${authUser.displayName}"
                HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 30)
                onComplete(true, null)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Sign in failed"
                _saveMessage.value = error
                onComplete(false, error)
            }
            _isSaving.value = false
        }
    }

    /**
     * Backward-compatible alias for existing callers.
     */
    fun signIn(email: String = "adelekesam10@gmail.com", name: String = "Sam Adeleke") {
        signInWithGoogle(email = email, name = name)
    }

    fun updateProfilePicture(uriString: String?) {
        viewModelScope.launch {
            if (uriString == null) {
                val current = settingsRepository.settings.value.profilePictureUri
                if (current != null) {
                    try {
                        val file = File(current)
                        if (file.exists()) file.delete()
                    } catch (_: Exception) {}
                }
                settingsRepository.setProfilePictureUri(null)
            } else {
                withContext(Dispatchers.IO) {
                    try {
                        val context = getApplication<Application>()
                        val sourceUri = Uri.parse(uriString)
                        val destFile = File(context.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")

                        // Clean up previous avatar file if exists
                        val oldUri = settingsRepository.settings.value.profilePictureUri
                        if (oldUri != null) {
                            try {
                                val oldFile = File(oldUri)
                                if (oldFile.exists()) oldFile.delete()
                            } catch (_: Exception) {}
                        }

                        context.contentResolver.openInputStream(sourceUri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        settingsRepository.setProfilePictureUri(destFile.absolutePath)
                    } catch (_: Exception) {
                        // If file copying fails, save the uriString directly
                        settingsRepository.setProfilePictureUri(uriString)
                    }
                }
            }
        }
    }

    fun updateThemeMode(mode: String) {
        settingsRepository.setThemeMode(mode)
    }

    fun updateTimeZone(timeZoneId: String) {
        settingsRepository.setConfiguredTimeZone(timeZoneId)
        val newToday = DateTimeUtils.getToday(timeZoneId)
        _selectedDate.value = newToday
        _currentMonth.value = YearMonth.from(newToday)
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        settingsRepository.setReminderEnabled(enabled)
        settingsRepository.setReminderTime(hour, minute)
    }

    fun updateHaptics(enabled: Boolean) {
        settingsRepository.setHapticsEnabled(enabled)
    }

    fun updateSupabase(url: String, key: String) {
        settingsRepository.setSupabaseConfig(url, key)
    }

    fun testReminderNotification() {
        ReminderManager.showNotification(getApplication())
        HapticUtils.performHaptic(getApplication(), settings.value.hapticsEnabled, 25)
    }

    fun clearSaveMessage() {
        _saveMessage.value = null
    }
}
