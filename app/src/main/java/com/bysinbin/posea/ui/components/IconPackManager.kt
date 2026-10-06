package com.bysinbin.posea.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.bysinbin.posea.model.IconPackInfo

object IconPackManager {

    private val ICON_PACK_ACTIONS = listOf(
        "com.novalauncher.THEME",
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "com.teslacoilsw.launcher.THEME",
        "com.fede.launcher.THEME_ICONPACK",
        "com.anddoes.launcher.THEME"
    )

    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val list = mutableListOf<IconPackInfo>()
        val seen = mutableSetOf<String>()

        for (action in ICON_PACK_ACTIONS) {
            val intent = Intent(action)
            val resolveInfos = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (!seen.contains(pkg)) {
                    seen.add(pkg)
                    val label = info.loadLabel(pm).toString()
                    list.add(IconPackInfo(packageName = pkg, label = label))
                }
            }
        }
        return list.sortedBy { it.label.lowercase() }
    }

    fun loadIconFromPack(
        context: Context,
        iconPackPackage: String,
        targetPackageName: String
    ): Drawable? {
        return try {
            val pm = context.packageManager
            val packRes = pm.getResourcesForApplication(iconPackPackage)
            // Çoğu ikon paketi drawable adını paket adı formatında saklar (örn: com_android_chrome)
            val resName = targetPackageName.replace(".", "_").lowercase()
            val resId = packRes.getIdentifier(resName, "drawable", iconPackPackage)
            if (resId != 0) {
                packRes.getDrawable(resId, null)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
