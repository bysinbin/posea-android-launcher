package com.bysinbin.posea.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppModel,
    iconStyle: IconStyle,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    onToggleFavorite: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val click = remember(app, onAppClick) { { onAppClick(app) } }
    val longClick = remember(app, onAppLongClick) { { onAppLongClick(app) } }
    val toggleFav: (() -> Unit)? = remember(app.packageName, onToggleFavorite) {
        if (onToggleFavorite != null) { { onToggleFavorite(app.packageName) } } else null
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = click,
                onLongClick = longClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                app = app,
                iconStyle = iconStyle,
                size = 44.dp,
                badgeCount = badgeCount
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (toggleFav != null) {
                IconButton(onClick = toggleFav) {
                    Icon(
                        imageVector = if (app.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorilere Ekle/Kaldır",
                        tint = if (app.isFavorite) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridItem(
    app: AppModel,
    iconStyle: IconStyle,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val click = remember(app, onAppClick) { { onAppClick(app) } }
    val longClick = remember(app, onAppLongClick) { { onAppLongClick(app) } }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = click,
                onLongClick = longClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppIcon(
            app = app,
            iconStyle = iconStyle,
            size = 52.dp,
            badgeCount = badgeCount
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
