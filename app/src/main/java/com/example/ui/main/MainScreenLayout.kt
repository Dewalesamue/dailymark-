package com.example.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.NavigationTab
import com.example.ui.theme.Accent
import com.example.ui.theme.AshGrey
import com.example.ui.theme.Charcoal
import com.example.ui.theme.Porcelain

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
        selectedIconColor = Accent,
        selectedTextColor = Accent,
        indicatorColor = Accent.copy(alpha = 0.18f),
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
                        Nav3DIcon(
                            drawableRes = R.drawable.img_3d_nav_home_1790179777958,
                            fallbackVector = if (selectedTab == NavigationTab.HOME) Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Home",
                            isSelected = selectedTab == NavigationTab.HOME
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
                        Nav3DIcon(
                            drawableRes = R.drawable.img_3d_calendar_1790178917428,
                            fallbackVector = if (selectedTab == NavigationTab.CALENDAR) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Calendar",
                            isSelected = selectedTab == NavigationTab.CALENDAR
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
                        Nav3DIcon(
                            drawableRes = R.drawable.img_3d_nav_memories_1790179789508,
                            fallbackVector = if (selectedTab == NavigationTab.MEMORIES) Icons.Default.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                            contentDescription = "Memories",
                            isSelected = selectedTab == NavigationTab.MEMORIES
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
                        Nav3DIcon(
                            drawableRes = R.drawable.img_3d_nav_profile_1790179800993,
                            fallbackVector = if (selectedTab == NavigationTab.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                            contentDescription = "Profile",
                            isSelected = selectedTab == NavigationTab.PROFILE
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
                    containerColor = Accent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("fab_camera_button")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_3d_shutter_fab_1790179812125),
                        contentDescription = "Capture today's moment",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
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

@Composable
private fun Nav3DIcon(
    drawableRes: Int,
    fallbackVector: ImageVector,
    contentDescription: String,
    isSelected: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 0.90f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_icon_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.65f,
        label = "nav_icon_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) Modifier.border(1.5.dp, Accent, CircleShape)
                    else Modifier
                )
        )
    }
}

