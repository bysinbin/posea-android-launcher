package com.bysinbin.posea.ui.components

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconBitmapCache = LruCache<String, Bitmap>(300)

/**
 * Android Drawable nesnesini Bitmap'e dönüştürür ve LruCache içinde saklar.
 */
fun getOrCacheBitmap(packageName: String, drawable: Drawable?, targetSize: Int = 144): Bitmap? {
    if (drawable == null) return null
    val cached = iconBitmapCache.get(packageName)
    if (cached != null) return cached

    val bitmap = try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetSize
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetSize
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
    } catch (e: Exception) {
        null
    }

    if (bitmap != null) {
        iconBitmapCache.put(packageName, bitmap)
    }
    return bitmap
}

/**
 * Arka planda asenkron olarak uygulama ikonunu çeker.
 */
fun loadAppBitmap(context: Context, app: AppModel, targetSize: Int = 144): Bitmap? {
    val cached = iconBitmapCache.get(app.packageName)
    if (cached != null) return cached

    val drawable = if (app.icon != null) {
        app.icon
    } else {
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val activities = launcherApps.getActivityList(app.packageName, app.userHandle ?: Process.myUserHandle())
            activities.firstOrNull()?.getBadgedIcon(0) ?: context.packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {
            try {
                context.packageManager.getApplicationIcon(app.packageName)
            } catch (_: Exception) {
                null
            }
        }
    }

    return getOrCacheBitmap(app.packageName, drawable, targetSize)
}

@Composable
fun AppIcon(
    app: AppModel,
    iconStyle: IconStyle,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    if (iconStyle == IconStyle.TEXT_ONLY) {
        // Tipografik minimal gösterim: Yuvarlak harf rozeti
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

    val context = LocalContext.current
    var bitmap by remember(app.packageName) {
        mutableStateOf(iconBitmapCache.get(app.packageName))
    }

    LaunchedEffect(app.packageName) {
        if (bitmap == null) {
            bitmap = withContext(Dispatchers.IO) {
                loadAppBitmap(context, app)
            }
        }
    }

    if (bitmap != null) {
        val colorFilter = remember(iconStyle) {
            when (iconStyle) {
                IconStyle.MONOCHROME -> {
                    val matrix = ColorMatrix().apply {
                        setToSaturation(0f)
                    }
                    ColorFilter.colorMatrix(matrix)
                }
                else -> null
            }
        }

        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = app.label,
            colorFilter = colorFilter,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f))
        )
    } else {
        // İkon arka planda yüklenirken anlık gösterilen hafif harf rozeti
        val initialLetter = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
