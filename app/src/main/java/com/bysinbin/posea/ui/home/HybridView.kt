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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle

@Composable
fun HybridView(
    favoriteApps: List<AppModel>,
    allApps: List<AppModel>,
    iconStyle: IconStyle,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> MinimalistView(
                    favoriteApps = favoriteApps,
                    iconStyle = iconStyle,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    onOpenDrawer = onOpenDrawer
                )
                1 -> StandardGridView(
                    apps = allApps,
                    dockApps = favoriteApps,
                    iconStyle = iconStyle,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    onOpenDrawer = onOpenDrawer
                )
            }
        }

        // Sayfa Gösterge Noktaları (Pager Indicator)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                val isSelected = pagerState.currentPage == index
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
}
