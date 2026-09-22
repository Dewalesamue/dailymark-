package com.example.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NavigationTab
import com.example.ui.theme.AshGrey
import com.example.ui.theme.Charcoal
import com.example.ui.theme.Porcelain
import com.example.ui.theme.SandyClay
import com.example.ui.theme.SunlitClay

/**
 * Main screen layout featuring a BottomNavigation bar and a placeholder/pluggable content area
 * to support flexible view switching across tabs.
 */
@Composable
fun MainScreenLayout(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    onFabClick: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    modifier: Modifier = Modifier,
    content: @Composable (innerPadding: PaddingValues) -> Unit
) {
    val isDarkNav = MaterialTheme.colorScheme.background.red < 0.5f
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = if (isDarkNav) Porcelain else Charcoal,
        selectedTextColor = if (isDarkNav) Porcelain else Charcoal,
        indicatorColor = if (isDarkNav) SandyClay.copy(alpha = 0.45f) else SunlitClay.copy(alpha = 0.45f),
        unselectedIconColor = if (isDarkNav) AshGrey else Charcoal.copy(alpha = 0.55f),
        unselectedTextColor = if (isDarkNav) AshGrey else Charcoal.copy(alpha = 0.65f)
    )

    Scaffold(
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = if (isDarkNav) Charcoal else Porcelain,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("main_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.HOME,
                    onClick = { onTabSelected(NavigationTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.HOME)
                                Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home", fontSize = 11.sp) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_home")
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.CALENDAR,
                    onClick = { onTabSelected(NavigationTab.CALENDAR) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.CALENDAR)
                                Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Calendar"
                        )
                    },
                    label = { Text("Calendar", fontSize = 11.sp) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_calendar")
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.MEMORIES,
                    onClick = { onTabSelected(NavigationTab.MEMORIES) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.MEMORIES)
                                Icons.Default.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                            contentDescription = "Memories"
                        )
                    },
                    label = { Text("Memories", fontSize = 11.sp) },
                    colors = navItemColors,
                    modifier = Modifier.testTag("nav_tab_memories")
                )

                NavigationBarItem(
                    selected = selectedTab == NavigationTab.PROFILE,
                    onClick = { onTabSelected(NavigationTab.PROFILE) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == NavigationTab.PROFILE)
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
            if (onFabClick != null) {
                FloatingActionButton(
                    onClick = onFabClick,
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
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("main_screen_content_area")
        ) {
            content(innerPadding)
        }
    }
}
