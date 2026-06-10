package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdhanViewModel
import com.example.ui.theme.*

enum class AdhanTab(val title: String, val icon: String, val testTag: String) {
    PRAYERS("الأوقات", "🕌", "tab_prayers"),
    QIBLA("القبلة", "🕋", "tab_qibla"),
    TRACKER("المتابع", "✅", "tab_tracker"),
    ROSARY("المسبحة", "📿", "tab_rosary"),
    SETTINGS("الإعدادات", "⚙️", "tab_settings")
}

@Composable
fun MainAppScreen(viewModel: AdhanViewModel) {
    var currentTab by remember { mutableStateOf(AdhanTab.PRAYERS) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkStarryBg,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = DarkSurfaceCard,
                tonalElevation = 8.dp
            ) {
                AdhanTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Text(
                                text = tab.icon,
                                fontSize = 22.sp
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryGold,
                            selectedTextColor = PrimaryGold,
                            indicatorColor = PrimaryGold.copy(alpha = 0.15f),
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Smooth horizontal crossfade navigation
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { targetTab ->
                when (targetTab) {
                    AdhanTab.PRAYERS -> PrayerTimesScreen(viewModel)
                    AdhanTab.QIBLA -> QiblaScreen(viewModel)
                    AdhanTab.TRACKER -> TrackerScreen(viewModel)
                    AdhanTab.ROSARY -> RosaryScreen(viewModel)
                    AdhanTab.SETTINGS -> SettingsScreen(viewModel)
                }
            }
        }
    }
}
