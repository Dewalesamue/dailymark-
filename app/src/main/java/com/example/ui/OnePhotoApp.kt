package com.example.ui

import android.app.Activity
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.calendar.CalendarScreen
import com.example.ui.camera.CameraCaptureScreen
import com.example.ui.components.SavingProgressDialog
import com.example.ui.detail.PhotoDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.main.MainScreenLayout
import com.example.ui.memories.MemoriesScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.photo_confirm.PhotoConfirmScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.Accent
import com.example.ui.theme.Charcoal
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Porcelain
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnePhotoApp(
    viewModel: JournalViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val showGoogleOAuthDialog by viewModel.showGoogleOAuthDialog.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    val isDark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSaveMessage()
        }
    }

    MyApplicationTheme(darkTheme = isDark) {
        when (val screen = uiState.currentScreen) {
            is Screen.Onboarding -> {
                BackHandler {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressTime < 2000L) {
                        (context as? Activity)?.finish()
                    } else {
                        lastBackPressTime = currentTime
                        Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                }
                OnboardingScreen(
                    onComplete = { viewModel.completeOnboarding() }
                )
            }

            is Screen.Auth -> {
                BackHandler {
                    if (settings.isSignedIn) {
                        viewModel.closeAuthScreen()
                    } else {
                        viewModel.continueAsGuest()
                    }
                }
                AuthScreen(
                    onSignUpWithEmail = { name, email, password, onResult ->
                        viewModel.signUpWithEmail(name, email, password, onResult)
                    },
                    onSignInWithEmail = { email, password, onResult ->
                        viewModel.signInWithEmail(email, password, onResult)
                    },
                    onSignInWithGoogle = { email, name, onResult ->
                        viewModel.signInWithGoogle(email = email, name = name, onComplete = onResult)
                    },
                    onStartGoogleOAuth = {
                        viewModel.startGoogleOAuth(context)
                    },
                    onStartGoogleWebOAuth = {
                        viewModel.startGoogleWebOAuth(context)
                    },
                    onContinueAsGuest = { viewModel.continueAsGuest() },
                    onBack = if (settings.isSignedIn) { { viewModel.closeAuthScreen() } } else null,
                    initialEmail = if (settings.isSignedIn) settings.userEmail else ""
                )
            }

            is Screen.CameraCapture -> {
                BackHandler { viewModel.selectTab(NavigationTab.HOME) }
                CameraCaptureScreen(
                    targetDate = viewModel.getTodayDate(),
                    onPhotoCaptured = { bitmap ->
                        viewModel.onCameraPhotoCaptured(bitmap)
                    },
                    onVideoCaptured = { uri ->
                        viewModel.onCameraVideoCaptured(uri)
                    },
                    onGalleryPick = { uris ->
                        viewModel.handleGalleryPick(uris, viewModel.getTodayDate())
                    },
                    onClose = { viewModel.selectTab(NavigationTab.HOME) }
                )
            }

            is Screen.PhotoConfirm -> {
                BackHandler { viewModel.selectTab(NavigationTab.HOME) }
                PhotoConfirmScreen(
                    bitmap = screen.bitmap,
                    uri = screen.uri,
                    targetDate = screen.targetDate,
                    isFromCamera = screen.isFromCamera,
                    isVideo = screen.isVideo,
                    suggestedExifDate = screen.suggestedExifDate,
                    currentDayCount = screen.currentDayCount,
                    isSaving = uiState.isSaving,
                    onRetake = {
                        if (screen.isFromCamera) {
                            viewModel.openCameraForToday()
                        } else {
                            viewModel.selectTab(NavigationTab.HOME)
                        }
                    },
                    onSave = { effectiveDate, caption, mood ->
                        if (screen.isVideo && screen.uri != null) {
                            viewModel.saveVideo(
                                uri = screen.uri,
                                targetDate = effectiveDate,
                                caption = caption,
                                mood = mood,
                                onSuccess = {}
                            )
                        } else {
                            viewModel.saveConfirmedPhoto(
                                bitmap = screen.bitmap,
                                uri = screen.uri,
                                targetDate = effectiveDate,
                                caption = caption,
                                mood = mood,
                                onSuccess = {}
                            )
                        }
                    }
                )
            }

            is Screen.PhotoDetail -> {
                BackHandler { viewModel.closeToTabs() }
                PhotoDetailScreen(
                    targetDate = screen.targetDate,
                    initialPhotoId = screen.initialPhotoId,
                    allPhotos = uiState.photos,
                    today = viewModel.getTodayDate(),
                    onClose = { viewModel.closeToTabs() },
                    onUpdateCaptionAndMood = { photo, caption, mood ->
                        viewModel.updateCaptionAndMood(photo, caption, mood)
                    },
                    onDeletePhoto = { photo, onDeleted ->
                        viewModel.deletePhoto(photo, onDeleted)
                    }
                )
            }

            is Screen.Notifications -> {
                BackHandler { viewModel.closeToTabs() }
                NotificationsScreen(
                    settings = settings,
                    onUpdateReminder = { enabled, hour, minute ->
                        viewModel.updateReminder(enabled, hour, minute)
                    },
                    onUpdateHaptics = { enabled ->
                        viewModel.updateHaptics(enabled)
                    },
                    onTestNotification = {
                        viewModel.testReminderNotification()
                    },
                    onBack = { viewModel.closeToTabs() }
                )
            }

            is Screen.MainTabs -> {
                // App-wide Back Navigation:
                // If not on HOME tab, single back navigates to HOME.
                // If on HOME tab, exit requires pressing back twice in 2 seconds.
                if (uiState.currentTab != NavigationTab.HOME) {
                    BackHandler {
                        viewModel.selectTab(NavigationTab.HOME)
                    }
                } else {
                    BackHandler {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000L) {
                            (context as? Activity)?.finish()
                        } else {
                            lastBackPressTime = currentTime
                            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                MainScreenLayout(
                    selectedTab = uiState.currentTab,
                    onTabSelected = { tab -> viewModel.selectTab(tab) },
                    onFabClick = { viewModel.openCameraForToday() },
                    snackbarHostState = snackbarHostState
                ) { innerPadding ->
                    when (uiState.currentTab) {
                        NavigationTab.HOME -> {
                            HomeScreen(
                                uiState = uiState,
                                userName = settings.userName,
                                profilePictureUri = settings.profilePictureUri,
                                onOpenCamera = { viewModel.openCameraForToday() },
                                onSelectGalleryUris = { uris ->
                                    viewModel.handleGalleryPick(uris, viewModel.getTodayDate())
                                },
                                onOpenPhotoDetail = { id ->
                                    val photo = uiState.photos.find { it.id == id }
                                    val dateStr = photo?.journalDate ?: DateTimeUtils.toIsoDate(viewModel.getTodayDate())
                                    viewModel.openDetailForDate(dateStr, id)
                                },
                                onOpenSettings = {
                                    viewModel.selectTab(NavigationTab.PROFILE)
                                },
                                onOpenProfile = {
                                    viewModel.selectTab(NavigationTab.PROFILE)
                                },
                                onNavigateToCalendar = {
                                    viewModel.selectTab(NavigationTab.CALENDAR)
                                },
                                onNavigateToMemories = {
                                    viewModel.selectTab(NavigationTab.MEMORIES)
                                },
                                onOpenNotifications = {
                                    viewModel.openNotifications()
                                }
                            )
                        }

                        NavigationTab.CALENDAR -> {
                            CalendarScreen(
                                uiState = uiState,
                                onNavigateMonth = { delta -> viewModel.navigateMonth(delta) },
                                onJumpToToday = { viewModel.jumpToToday() },
                                onSelectDate = { date -> viewModel.selectDate(date) },
                                onOpenPhotoDetail = { id ->
                                    val photo = uiState.photos.find { it.id == id }
                                    val dateStr = photo?.journalDate ?: DateTimeUtils.toIsoDate(uiState.selectedDate)
                                    viewModel.openDetailForDate(dateStr, id)
                                },
                                onCaptureForDate = { date ->
                                    viewModel.openCameraForDate(date)
                                },
                                onPickForDate = { date, uris ->
                                    viewModel.handleGalleryPick(uris, date)
                                },
                                onDismissActionSheet = {
                                    viewModel.dismissDateActionSheet()
                                }
                            )
                        }

                        NavigationTab.MEMORIES -> {
                            MemoriesScreen(
                                uiState = uiState,
                                today = viewModel.getTodayDate(),
                                onOpenPhotoDetail = { id, dateStr ->
                                    viewModel.openDetailForDate(dateStr, id)
                                },
                                onUpdateCustomName = { date, name ->
                                    viewModel.setDayCustomTitle(date, name)
                                }
                            )
                        }

                        NavigationTab.PROFILE -> {
                            ProfileScreen(
                                uiState = uiState,
                                settings = settings,
                                onUpdateUserName = { name -> viewModel.updateUserName(name) },
                                onUpdateUserEmail = { email -> viewModel.updateUserEmail(email) },
                                onUpdateProfilePicture = { uri -> viewModel.updateProfilePicture(uri) },
                                onUpdateTheme = { theme -> viewModel.updateThemeMode(theme) },
                                onUpdateReminder = { enabled, hour, min ->
                                    viewModel.updateReminder(enabled, hour, min)
                                },
                                onUpdateHaptics = { enabled -> viewModel.updateHaptics(enabled) },
                                onTestNotification = { viewModel.testReminderNotification() },
                                onSignOut = { viewModel.signOut() },
                                onSignIn = { viewModel.openAuthScreen() },
                                onResetOnboarding = { viewModel.resetOnboarding() }
                            )
                        }
                    }

                    // Progress dialog when saving multiple photos
                    uiState.savingProgress?.let { progress ->
                        SavingProgressDialog(
                            progress = progress
                        )
                    }
                }
            }
        }

        // In-App Google OAuth Sheet / Dialog
        if (showGoogleOAuthDialog) {
            Dialog(
                onDismissRequest = { viewModel.closeGoogleOAuthDialog() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopAppBar(
                            title = { Text("Google Sign-In", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { viewModel.closeGoogleOAuthDialog() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        var webProgress by remember { mutableFloatStateOf(0.1f) }
                        if (webProgress < 1f) {
                            LinearProgressIndicator(
                                progress = { webProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp),
                                color = Accent
                            )
                        }
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    val webSettings = this.settings
                                    webSettings.javaScriptEnabled = true
                                    webSettings.domStorageEnabled = true
                                    val defaultUa = webSettings.userAgentString
                                    webSettings.userAgentString = defaultUa.replace("; wv", "").replace("Version/4.0 ", "")
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            webProgress = newProgress / 100f
                                        }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val url = request?.url?.toString() ?: return false
                                            return handleOAuthUrl(url)
                                        }

                                        @Deprecated("Deprecated in Java")
                                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                            if (url == null) return false
                                            return handleOAuthUrl(url)
                                        }

                                        private fun handleOAuthUrl(url: String): Boolean {
                                            if (url.contains("access_token=") || url.startsWith("onephotoday://") || url.contains("auth-callback")) {
                                                val parsedUri = Uri.parse(url)
                                                viewModel.handleOAuthCallback(parsedUri)
                                                return true
                                            }
                                            return false
                                        }
                                    }
                                    loadUrl(viewModel.supabaseService.getGoogleOAuthUrl())
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
