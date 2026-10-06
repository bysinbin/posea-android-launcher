package com.bysinbin.posea.data.system

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

@Immutable
data class PoseaShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val iconBitmap: ImageBitmap? = null,
    val shortcutInfo: ShortcutInfo
)

object AppShortcutsManager {

    fun hasShortcutPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
        return try {
            launcherApps?.hasShortcutHostPermission() == true
        } catch (_: Exception) {
            false
        }
    }

    fun getShortcuts(context: Context, packageName: String): List<PoseaShortcut> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return emptyList()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return emptyList()

        return try {
            if (!launcherApps.hasShortcutHostPermission()) return emptyList()

            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            }
            val shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
            val density = context.resources.displayMetrics.densityDpi

            shortcuts.take(4).map { info ->
                val label = info.shortLabel?.toString()
                    ?: info.longLabel?.toString()
                    ?: "Kısayol"
                val drawable = try {
                    launcherApps.getShortcutIconDrawable(info, density)
                } catch (_: Exception) {
                    null
                }
                val bmp = drawableToImageBitmap(drawable)
                PoseaShortcut(
                    id = info.id,
                    packageName = packageName,
                    label = label,
                    iconBitmap = bmp,
                    shortcutInfo = info
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun launchShortcut(context: Context, shortcut: PoseaShortcut) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return
        try {
            launcherApps.startShortcut(shortcut.shortcutInfo, null, null)
        } catch (_: Exception) {
            try {
                context.packageManager.getLaunchIntentForPackage(shortcut.packageName)?.let {
                    context.startActivity(it)
                }
            } catch (_: Exception) {}
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable?): ImageBitmap? {
        if (drawable == null) return null
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                drawable.bitmap.asImageBitmap()
            } else {
                val width = if (drawable.intrinsicWidth in 1..256) drawable.intrinsicWidth else 96
                val height = if (drawable.intrinsicHeight in 1..256) drawable.intrinsicHeight else 96
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}
