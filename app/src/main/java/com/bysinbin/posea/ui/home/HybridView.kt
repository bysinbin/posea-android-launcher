package com.bysinbin.posea.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bysinbin.posea.data.system.LauncherWidgetManager
import com.bysinbin.posea.model.AppFolder
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle

@Composable
fun HybridView(
    favoriteApps: List<AppModel>,
    allApps: List<AppModel>,
    folders: List<AppFolder> = emptyList(),
    gridColumns: Int = 4,
    iconStyle: IconStyle,
    clockStyle: com.bysinbin.posea.model.ClockStyle = com.bysinbin.posea.model.ClockStyle.DIGITAL,
    hideAppLabels: Boolean = false,
    homeWidgetIds: List<Int> = emptyList(),
    pinnedWidgetIds: List<Int> = emptyList(),
    widgetManager: LauncherWidgetManager,
    badgeCounts: Map<String, Int> = emptyMap(),
    onAddHomeWidgetClick: () -> Unit,
    onRemoveHomeWidget: (Int) -> Unit,
    onAddPinnedWidgetClick: () -> Unit,
    onRemovePinnedWidget: (Int) -> Unit,
    onFolderClick: (AppFolder) -> Unit = {},
    onFolderLongClick: (AppFolder) -> Unit = {},
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onExpandNotifications: () -> Unit = {},
    onHomeScreenLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    Column(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            flingBehavior = flingBehavior,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> MinimalistView(
                    favoriteApps = favoriteApps,
                    iconStyle = iconStyle,
                    clockStyle = clockStyle,
                    homeWidgetIds = homeWidgetIds,
                    widgetManager = widgetManager,
                    badgeCounts = badgeCounts,
                    onAddWidgetClick = onAddHomeWidgetClick,
                    onRemoveWidget = onRemoveHomeWidget,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    onOpenDrawer = onOpenDrawer,
                    onOpenSettings = onOpenSettings,
                    onExpandNotifications = onExpandNotifications,
                    onHomeScreenLongClick = onHomeScreenLongClick
                )
                1 -> StandardGridView(
                    apps = allApps,
                    dockApps = favoriteApps,
                    folders = folders,
                    gridColumns = gridColumns,
                    iconStyle = iconStyle,
                    clockStyle = clockStyle,
                    hideAppLabels = hideAppLabels,
                    pinnedWidgetIds = pinnedWidgetIds,
                    widgetManager = widgetManager,
                    badgeCounts = badgeCounts,
                    onAddWidgetClick = onAddPinnedWidgetClick,
                    onRemoveWidget = onRemovePinnedWidget,
                    onFolderClick = onFolderClick,
                    onFolderLongClick = onFolderLongClick,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    onOpenDrawer = onOpenDrawer,
                    onOpenSettings = onOpenSettings,
                    onExpandNotifications = onExpandNotifications,
                    onHomeScreenLongClick = onHomeScreenLongClick
                )
            }
        }

        // Sayfa Gösterge Noktaları (Pager Indicator - Ayrı Composable ile Sıfır Recomposition)
        HybridPagerIndicator(currentPage = pagerState.currentPage)
    }
}

@Composable
private fun HybridPagerIndicator(currentPage: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(2) { index ->
            val isSelected = currentPage == index
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 16.dp else 6.dp, 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    )
            )
        }
    }
}
