package com.bysinbin.posea.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val imageBitmapCache = LruCache<String, ImageBitmap>(400)
    private val monochromeFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    var activeIconPack: String? = null

    fun clearCache() {
        imageBitmapCache.evictAll()
    }

    fun get(packageName: String): ImageBitmap? = imageBitmapCache.get(packageName)

    fun getMonochromeFilter(): ColorFilter = monochromeFilter

    fun loadAndCache(context: Context, packageName: String): ImageBitmap? {
        val cached = imageBitmapCache.get(packageName)
        if (cached != null) return cached

        val drawable = if (activeIconPack != null) {
            IconPackManager.loadIconFromPack(context, activeIconPack!!, packageName)
                ?: try { context.packageManager.getApplicationIcon(packageName) } catch (_: Exception) { null }
        } else {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        } ?: return null

        val imageBmp = if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            drawable.bitmap.asImageBitmap()
        } else {
            val width = if (drawable.intrinsicWidth in 1..256) drawable.intrinsicWidth else 120
            val height = if (drawable.intrinsicHeight in 1..256) drawable.intrinsicHeight else 120
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        }

        imageBitmapCache.put(packageName, imageBmp)
        return imageBmp
    }

    suspend fun preload(context: Context, apps: List<AppModel>) = withContext(Dispatchers.IO) {
        val uncached = apps.filter { imageBitmapCache.get(it.packageName) == null }
        if (uncached.isEmpty()) return@withContext

        // Maksimum 6 paralel parçalı hızlı önbelleğe alma
        uncached.chunked(6).forEach { chunk ->
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
    size: Dp = 48.dp
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
            contentDescription = app.label,
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
            contentDescription = app.label,
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
