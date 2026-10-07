package com.bysinbin.posea.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bysinbin.posea.data.system.PoseaNotificationService
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * 120 FPS ultra akıcı ikon önbellek yöneticisi.
 * İkonlar doğrudan ImageBitmap olarak saklanır, kaydırma sırasında dönüşüm maliyetini sıfıra indirir.
 */
object IconCacheManager {
    val imageBitmapCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()
    private val monochromeFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    var activeIconPack: String? = null

    private fun getIconDir(context: Context): java.io.File {
        val dir = java.io.File(context.cacheDir, "icon_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun clearCache(context: Context? = null) {
        imageBitmapCache.clear()
        if (context != null) {
            try {
                getIconDir(context).deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    fun get(packageName: String): ImageBitmap? = imageBitmapCache[packageName]

    fun getMonochromeFilter(): ColorFilter = monochromeFilter

    fun loadAndCache(context: Context, packageName: String): ImageBitmap {
        val cached = imageBitmapCache[packageName]
        if (cached != null) return cached

        // 1. Disk önbelleğini kontrol et (Eğer özel ikon paketi aktif değilse)
        val iconFile = if (activeIconPack == null) {
            java.io.File(getIconDir(context), "${packageName}.png")
        } else null

        if (iconFile != null && iconFile.exists() && iconFile.length() > 0) {
            try {
                val diskBmp = android.graphics.BitmapFactory.decodeFile(iconFile.absolutePath)
                if (diskBmp != null) {
                    val imageBmp = diskBmp.asImageBitmap()
                    imageBitmapCache[packageName] = imageBmp
                    return imageBmp
                }
            } catch (_: Exception) {}
        }

        // 2. Sistemden veya İkon Paketinden İkonu Çek
        val drawable = if (activeIconPack != null) {
            IconPackManager.loadIconFromPack(context, activeIconPack!!, packageName)
                ?: try { context.packageManager.getApplicationIcon(packageName) } catch (_: Exception) { null }
        } else {
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? android.content.pm.LauncherApps
                val activities = launcherApps?.getActivityList(packageName, android.os.Process.myUserHandle())
                activities?.firstOrNull()?.getIcon(context.resources.displayMetrics.densityDpi)
                    ?: context.packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        }

        val density = context.resources.displayMetrics.density
        val targetPx = (52 * density).toInt().coerceIn(96, 160)

        val bmp = if (drawable != null) {
            try {
                val b = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(b)
                drawable.setBounds(0, 0, targetPx, targetPx)
                drawable.draw(canvas)

                // Disk önbelleğine kaydet
                if (iconFile != null) {
                    try {
                        java.io.FileOutputStream(iconFile).use { out ->
                            b.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    } catch (_: Exception) {}
                }
                b
            } catch (_: Exception) {
                createFallbackBitmap(packageName, targetPx)
            }
        } else {
            createFallbackBitmap(packageName, targetPx)
        }

        val imageBmp = bmp.asImageBitmap()
        imageBitmapCache[packageName] = imageBmp
        return imageBmp
    }

    private fun createFallbackBitmap(packageName: String, sizePx: Int): Bitmap {
        val b = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(b)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = sizePx * 0.42f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        val letter = packageName.substringAfterLast(".").firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val yPos = (sizePx / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(letter, sizePx / 2f, yPos, paint)
        return b
    }

    suspend fun preload(context: Context, apps: List<AppModel>) = withContext(Dispatchers.IO) {
        val uncached = apps.filter { imageBitmapCache[it.packageName] == null }
        if (uncached.isEmpty()) return@withContext

        // Paralel parçalı hızlı önbelleğe alma (Diskten anında okunur)
        uncached.chunked(10).forEach { chunk ->
            chunk.map { app ->
                async {
                    loadAndCache(context, app.packageName)
                }
            }.awaitAll()
        }
    }
}

@Composable
fun AppIcon(
    app: AppModel,
    iconStyle: IconStyle,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    badgeCount: Int = 0
) {
    if (badgeCount <= 0) {
        AppIconContent(
            app = app,
            iconStyle = iconStyle,
            size = size,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            AppIconContent(
                app = app,
                iconStyle = iconStyle,
                size = size
            )

            val badgeSize = if (badgeCount > 9) 16.dp else 12.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (badgeCount > 1) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIconContent(
    app: AppModel,
    iconStyle: IconStyle,
    size: Dp,
    modifier: Modifier = Modifier
) {
    if (iconStyle == IconStyle.TEXT_ONLY) {
        val initialLetter = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialLetter,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        return
    }

    val cachedBmp = IconCacheManager.get(app.packageName)
    if (cachedBmp != null) {
        // HIZLI YOL: Sıfır coroutine, sıfır state, anında doğrudan çizim
        Image(
            bitmap = cachedBmp,
            contentDescription = null,
            colorFilter = if (iconStyle == IconStyle.MONOCHROME) IconCacheManager.getMonochromeFilter() else null,
            modifier = modifier.size(size)
        )
        return
    }

    // YAVAŞ YOL: Sadece ikon bellekte henüz yoksa
    val context = LocalContext.current
    var asyncBmp by remember(app.packageName) {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(app.packageName) {
        asyncBmp = withContext(Dispatchers.IO) {
            IconCacheManager.loadAndCache(context, app.packageName)
        }
    }

    val currentBmp = asyncBmp
    if (currentBmp != null) {
        Image(
            bitmap = currentBmp,
            contentDescription = null,
            colorFilter = if (iconStyle == IconStyle.MONOCHROME) IconCacheManager.getMonochromeFilter() else null,
            modifier = modifier.size(size)
        )
    } else {
        val initialLetter = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialLetter,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

