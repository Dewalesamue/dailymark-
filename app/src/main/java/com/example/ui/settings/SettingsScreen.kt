package com.example.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.repository.UserSettings
import com.example.ui.JournalUiState
import com.example.ui.profile.ProfileScreen

@Deprecated("Use ProfileScreen instead as Settings has been merged into Profile")
@Composable
fun SettingsScreen(
    uiState: JournalUiState,
    settings: UserSettings,
    onUpdateUserName: (String) -> Unit,
    onUpdateProfilePicture: (String?) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onUpdateReminder: (Boolean, Int, Int) -> Unit,
    onUpdateHaptics: (Boolean) -> Unit,
    onUpdateSupabase: ((String, String) -> Unit)? = null,
    onTestNotification: () -> Unit,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileScreen(
        uiState = uiState,
        settings = settings,
        onUpdateUserName = onUpdateUserName,
        onUpdateUserEmail = {},
        onUpdateProfilePicture = onUpdateProfilePicture,
        onUpdateTheme = onUpdateTheme,
        onUpdateReminder = onUpdateReminder,
        onUpdateHaptics = onUpdateHaptics,
        onTestNotification = onTestNotification,
        onSignOut = {},
        onSignIn = {},
        onResetOnboarding = onResetOnboarding,
        modifier = modifier
    )
}
