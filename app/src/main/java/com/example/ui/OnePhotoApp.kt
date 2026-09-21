package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.calendar.CalendarScreen
import com.example.ui.camera.CameraCaptureScreen
import com.example.ui.components.SavingProgressDialog
import com.example.ui.detail.PhotoDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.memories.MemoriesScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.photo_confirm.PhotoConfirmScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.AshGrey
import com.example.ui.theme.Charcoal
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Porcelain
import com.example.ui.theme.SandyClay
import com.example.ui.theme.SunlitClay
import com.example.util.DateTimeUtils

@Composable
fun OnePhotoApp(
    viewModel: JournalViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
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
                    onContinueAsGuest = { viewModel.continueAsGuest() },
                    onBack = if (settings.isSignedIn) { { viewModel.closeAuthScreen() } } else null,
                    initialEmail = settings.userEmail
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

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        val isDarkNav = MaterialTheme.colorScheme.background.red < 0.5f
                        val navItemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isDarkNav) Porcelain else Charcoal,
                            selectedTextColor = if (isDarkNav) Porcelain else Charcoal,
                            indicatorColor = if (isDarkNav) SandyClay.copy(alpha = 0.45f) else SunlitClay.copy(alpha = 0.45f),
                            unselectedIconColor = if (isDarkNav) AshGrey else Charcoal.copy(alpha = 0.55f),
                            unselectedTextColor = if (isDarkNav) AshGrey else Charcoal.copy(alpha = 0.65f)
                        )

                        NavigationBar(
                            containerColor = if (isDarkNav) Charcoal else Porcelain,
                            tonalElevation = 0.dp
                        ) {
                            NavigationBarItem(
                                selected = uiState.currentTab == NavigationTab.HOME,
                                onClick = { viewModel.selectTab(NavigationTab.HOME) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == NavigationTab.HOME)
                                            Icons.Default.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                label = { Text("Home", fontSize = 11.sp) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_tab_home")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == NavigationTab.CALENDAR,
                                onClick = { viewModel.selectTab(NavigationTab.CALENDAR) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == NavigationTab.CALENDAR)
                                            Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                                        contentDescription = "Calendar"
                                    )
                                },
                                label = { Text("Calendar", fontSize = 11.sp) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_tab_calendar")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == NavigationTab.MEMORIES,
                                onClick = { viewModel.selectTab(NavigationTab.MEMORIES) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == NavigationTab.MEMORIES)
                                            Icons.Default.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                                        contentDescription = "Memories"
                                    )
                                },
                                label = { Text("Memories", fontSize = 11.sp) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_tab_memories")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == NavigationTab.PROFILE,
                                onClick = { viewModel.selectTab(NavigationTab.PROFILE) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == NavigationTab.PROFILE)
                                            Icons.Default.Person else Icons.Outlined.Person,
                                        contentDescription = "Profile"
                                    )
                                },
                                label = { Text("Profile", fontSize = 11.sp) },
                                colors = navItemColors,
                                modifier = Modifier.testTag("nav_tab_profile")
                            )
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { viewModel.openCameraForToday() },
                            containerColor = SunlitClay,
                            contentColor = Charcoal,
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                            modifier = Modifier.testTag("fab_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture today's moment",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
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
        }
    }
}
