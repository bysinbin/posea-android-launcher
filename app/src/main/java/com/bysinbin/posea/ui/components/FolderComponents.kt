package com.bysinbin.posea.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bysinbin.posea.model.AppFolder
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle

/**
 * Masaüstü Izgarasında Klasör Simgesi
 * 2x2 küçük ikon önizlemesi ile modern launcher klasör görünümü
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridItem(
    folder: AppFolder,
    allApps: List<AppModel>,
    iconStyle: IconStyle,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val folderApps = remember(folder.packageNames, allApps) {
        val appMap = allApps.associateBy { it.packageName }
        folder.packageNames.mapNotNull { appMap[it] }.take(4)
    }

    Column(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Klasör Çerçevesi (2x2 İkon Önizlemesi)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.size(52.dp)
        ) {
            Box(
                modifier = Modifier.padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (folderApps.isEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            folderApps.getOrNull(0)?.let {
                                AppIcon(app = it, iconStyle = iconStyle, size = 18.dp)
                            }
                            folderApps.getOrNull(1)?.let {
                                AppIcon(app = it, iconStyle = iconStyle, size = 18.dp)
                            }
                        }
                        if (folderApps.size > 2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                folderApps.getOrNull(2)?.let {
                                    AppIcon(app = it, iconStyle = iconStyle, size = 18.dp)
                                }
                                folderApps.getOrNull(3)?.let {
                                    AppIcon(app = it, iconStyle = iconStyle, size = 18.dp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Klasör Açıldığında Gösterilen İletişim Kutusu
 */
@Composable
fun FolderDetailDialog(
    folder: AppFolder,
    allApps: List<AppModel>,
    iconStyle: IconStyle,
    onAppClick: (AppModel) -> Unit,
    onRenameFolder: (String) -> Unit,
    onDeleteFolder: () -> Unit,
    onAddAppsToFolder: (List<String>) -> Unit,
    onRemoveAppFromFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember(folder.name) { mutableStateOf(folder.name) }
    var isAddingApps by remember { mutableStateOf(false) }

    val folderApps = remember(folder.packageNames, allApps) {
        val appMap = allApps.associateBy { it.packageName }
        folder.packageNames.mapNotNull { appMap[it] }
    }

    if (isAddingApps) {
        // Klasöre yeni uygulama seçme ekranı
        FolderAddAppsDialog(
            allApps = allApps,
            existingPackages = folder.packageNames.toSet(),
            onConfirm = { selectedPackages ->
                onAddAppsToFolder(selectedPackages)
                isAddingApps = false
            },
            onDismiss = { isAddingApps = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (isRenaming) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        if (renameText.isNotBlank()) {
                            onRenameFolder(renameText.trim())
                        }
                        isRenaming = false
                    }) {
                        Text("Kaydet", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { isRenaming = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Yeniden Adlandır", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            onDeleteFolder()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Klasörü Sil", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(folderApps, key = { it.packageName }) { app ->
                        AppGridItem(
                            app = app,
                            iconStyle = iconStyle,
                            onAppClick = {
                                onAppClick(app)
                                onDismiss()
                            },
                            onAppLongClick = {
                                onRemoveAppFromFolder(app.packageName)
                            }
                        )
                    }

                    // "+" Uygulama Ekle Butonu
                    item {
                        Column(
                            modifier = Modifier
                                .clickable { isAddingApps = true }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Uygulama Ekle",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ekle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = "İpucu: Klasörden çıkarmak için uygulamaya uzun basın.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

/**
 * Klasöre Çoklu Uygulama Ekleme Seçici
 */
@Composable
fun FolderAddAppsDialog(
    allApps: List<AppModel>,
    existingPackages: Set<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = remember { mutableStateOf(existingPackages.toMutableSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Klasöre Uygulama Ekle") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allApps, key = { it.packageName }) { app ->
                        val isChecked = selected.value.contains(app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val current = selected.value.toMutableSet()
                                    if (isChecked) current.remove(app.packageName)
                                    else current.add(app.packageName)
                                    selected.value = current
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { check ->
                                    val current = selected.value.toMutableSet()
                                    if (check) current.add(app.packageName)
                                    else current.remove(app.packageName)
                                    selected.value = current
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = app.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected.value.toList()) }) {
                Text("Tamam")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
