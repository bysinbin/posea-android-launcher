package com.bysinbin.posea.ui.drawer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.ui.components.AppGridItem
import com.bysinbin.posea.ui.components.AppListItem
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerSheet(
    isOpen: Boolean,
    searchQuery: String,
    filteredApps: List<AppModel>,
    recentApps: List<AppModel> = emptyList(),
    iconStyle: IconStyle,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // Arama Kutusu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            if (filteredApps.isNotEmpty()) {
                                onAppClick(filteredApps.first())
                                onDismiss()
                            }
                        }
                    ),
                    placeholder = {
                        Text(
                            text = "Uygulama ara...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Ara",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Temizle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                // Launcher Ayarları Butonu (Doğrudan Ayarları Açar)
                IconButton(
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                    modifier = Modifier.padding(end = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Launcher Ayarları",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val context = LocalContext.current
            val mathResult = remember(searchQuery) { MathEvaluator.evaluate(searchQuery) }

            // Arama "ayar" içeriyorsa hızlı Ayarlar kartı göster
            val isSearchingSettings = remember(searchQuery) {
                val q = searchQuery.trim().lowercase()
                q.isNotEmpty() && ("ayarlar".startsWith(q) || "settings".startsWith(q) || "launcher".startsWith(q))
            }
            if (isSearchingSettings) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            onDismiss()
                            onOpenSettings()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Launcher Ayarları",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Görünüm, ikonlar, widget'lar ve gizlilik ayarlarını aç",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Canlı Akıllı Hesap Makinesi Kartı
            if (mathResult != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Hesaplama Sonucu", mathResult))
                            Toast.makeText(context, "Kopyalandı: $mathResult", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hesaplama Sonucu",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "$searchQuery = $mathResult",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Hesaplama Sonucu", mathResult))
                            Toast.makeText(context, "Kopyalandı: $mathResult", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Kopyala",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            val onItemClick: (AppModel) -> Unit = remember(onAppClick, onDismiss) {
                { app ->
                    onAppClick(app)
                    onDismiss()
                }
            }

            // Son Kullanılanlar (Arama yapılmıyorsa göster)
            if (searchQuery.isBlank() && recentApps.isNotEmpty()) {
                Text(
                    text = "Son Kullanılanlar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    recentApps.take(4).forEach { app ->
                        AppGridItem(
                            app = app,
                            iconStyle = iconStyle,
                            onAppClick = onItemClick,
                            onAppLongClick = onAppLongClick
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    color = DividerDefaults.color.copy(alpha = 0.3f)
                )
            }

            // A-Z Harf İndeksi
            val alphabet = remember(filteredApps) {
                filteredApps.mapNotNull { it.label.firstOrNull()?.uppercaseChar() }.distinct()
            }

            // Uygulamalar Listesi ve Hızlı Harf Kaydırıcı
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp, end = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                        contentType = { "app_list_item" }
                    ) { app ->
                        AppListItem(
                            app = app,
                            iconStyle = iconStyle,
                            onAppClick = onItemClick,
                            onAppLongClick = onAppLongClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }

                    // Web'de Ara Butonu (Arama yapılıyorsa göster)
                    if (searchQuery.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                putExtra(android.app.SearchManager.QUERY, searchQuery)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                            onDismiss()
                                        } catch (_: Exception) {
                                            val browserIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://www.google.com/search?q=${java.net.URLEncoder.encode(searchQuery, "UTF-8")}")
                                            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            context.startActivity(browserIntent)
                                            onDismiss()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Google'da Ara",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "\"$searchQuery\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredApps.isEmpty() && mathResult == null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "'$searchQuery' ile eşleşen uygulama bulunamadı.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Sağ Taraf A-Z Hızlı Kaydırma Barı (Haptik geri bildirimli ve sürüklenebilir)
                if (alphabet.size > 2) {
                    var lastScrubbedLetter by remember { mutableStateOf<Char?>(null) }

                    Column(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .verticalScroll(rememberScrollState())
                            .pointerInput(alphabet) {
                                detectVerticalDragGestures(
                                    onDragStart = { offset ->
                                        val totalHeight = size.height
                                        if (totalHeight > 0 && alphabet.isNotEmpty()) {
                                            val itemHeight = totalHeight.toFloat() / alphabet.size
                                            val index = (offset.y / itemHeight).toInt().coerceIn(0, alphabet.lastIndex)
                                            val letter = alphabet[index]
                                            if (letter != lastScrubbedLetter) {
                                                lastScrubbedLetter = letter
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val targetIdx = filteredApps.indexOfFirst {
                                                    it.label.startsWith(letter, ignoreCase = true)
                                                }
                                                if (targetIdx >= 0) {
                                                    coroutineScope.launch { listState.scrollToItem(targetIdx) }
                                                }
                                            }
                                        }
                                    },
                                    onVerticalDrag = { change, _ ->
                                        change.consume()
                                        val totalHeight = size.height
                                        if (totalHeight > 0 && alphabet.isNotEmpty()) {
                                            val itemHeight = totalHeight.toFloat() / alphabet.size
                                            val index = (change.position.y / itemHeight).toInt().coerceIn(0, alphabet.lastIndex)
                                            val letter = alphabet[index]
                                            if (letter != lastScrubbedLetter) {
                                                lastScrubbedLetter = letter
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val targetIdx = filteredApps.indexOfFirst {
                                                    it.label.startsWith(letter, ignoreCase = true)
                                                }
                                                if (targetIdx >= 0) {
                                                    coroutineScope.launch { listState.scrollToItem(targetIdx) }
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = { lastScrubbedLetter = null },
                                    onDragCancel = { lastScrubbedLetter = null }
                                )
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        alphabet.forEach { letter ->
                            Text(
                                text = letter.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val targetIdx = filteredApps.indexOfFirst {
                                            it.label.startsWith(letter, ignoreCase = true)
                                        }
                                        if (targetIdx >= 0) {
                                            coroutineScope.launch {
                                                listState.scrollToItem(targetIdx)
                                            }
                                        }
                                    }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
