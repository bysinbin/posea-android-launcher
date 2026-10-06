package com.bysinbin.posea.data.system

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.bysinbin.posea.model.AppFolder
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.model.LauncherMode
import com.bysinbin.posea.model.UserPreferences
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    fun exportToJson(preferences: UserPreferences): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("mode", preferences.mode.name)
        root.put("iconStyle", preferences.iconStyle.name)
        root.put("gridColumns", preferences.gridColumns)
        root.put("isAmoledBlack", preferences.isAmoledBlack)
        root.put("isDynamicTheme", preferences.isDynamicTheme)
        root.put("showNotificationBadges", preferences.showNotificationBadges)
        root.put("showClock", preferences.showClock)
        preferences.selectedIconPackPackage?.let { root.put("iconPackPackage", it) }

        // Favoriler
        val favs = JSONArray()
        preferences.favoritePackages.forEach { favs.put(it) }
        root.put("favorites", favs)

        // Gizlenenler
        val hidden = JSONArray()
        preferences.hiddenPackages.forEach { hidden.put(it) }
        root.put("hidden", hidden)

        // Kilitliler
        val locked = JSONArray()
        preferences.lockedPackages.forEach { locked.put(it) }
        root.put("locked", locked)

        // Özel isimler
        val customNames = JSONObject()
        preferences.customAppNames.forEach { (pkg, name) -> customNames.put(pkg, name) }
        root.put("customNames", customNames)

        // Klasörler
        val foldersArray = JSONArray()
        preferences.folders.forEach { folder ->
            val fObj = JSONObject()
            fObj.put("id", folder.id)
            fObj.put("name", folder.name)
            val pkgs = JSONArray()
            folder.packageNames.forEach { pkgs.put(it) }
            fObj.put("packages", pkgs)
            foldersArray.put(fObj)
        }
        root.put("folders", foldersArray)

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): UserPreferences? {
        return try {
            val root = JSONObject(jsonString)

            val mode = try {
                LauncherMode.valueOf(root.optString("mode", LauncherMode.HYBRID.name))
            } catch (_: Exception) {
                LauncherMode.HYBRID
            }

            val iconStyle = try {
                IconStyle.valueOf(root.optString("iconStyle", IconStyle.COLOR.name))
            } catch (_: Exception) {
                IconStyle.COLOR
            }

            val gridCols = root.optInt("gridColumns", 4).coerceIn(3, 6)
            val isAmoled = root.optBoolean("isAmoledBlack", false)
            val isDynamic = root.optBoolean("isDynamicTheme", true)
            val showBadges = root.optBoolean("showNotificationBadges", true)
            val showClock = root.optBoolean("showClock", true)
            val iconPack = if (root.has("iconPackPackage")) root.getString("iconPackPackage") else null

            val favArray = root.optJSONArray("favorites")
            val favs = mutableSetOf<String>()
            if (favArray != null) {
                for (i in 0 until favArray.length()) favs.add(favArray.getString(i))
            }

            val hiddenArray = root.optJSONArray("hidden")
            val hidden = mutableSetOf<String>()
            if (hiddenArray != null) {
                for (i in 0 until hiddenArray.length()) hidden.add(hiddenArray.getString(i))
            }

            val lockedArray = root.optJSONArray("locked")
            val locked = mutableSetOf<String>()
            if (lockedArray != null) {
                for (i in 0 until lockedArray.length()) locked.add(lockedArray.getString(i))
            }

            val namesObj = root.optJSONObject("customNames")
            val customNames = mutableMapOf<String, String>()
            if (namesObj != null) {
                val keys = namesObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    customNames[k] = namesObj.getString(k)
                }
            }

            val foldersArray = root.optJSONArray("folders")
            val folders = mutableListOf<AppFolder>()
            if (foldersArray != null) {
                for (i in 0 until foldersArray.length()) {
                    val fObj = foldersArray.getJSONObject(i)
                    val id = fObj.getString("id")
                    val name = fObj.getString("name")
                    val pkgsArray = fObj.optJSONArray("packages")
                    val pkgs = mutableListOf<String>()
                    if (pkgsArray != null) {
                        for (j in 0 until pkgsArray.length()) pkgs.add(pkgsArray.getString(j))
                    }
                    folders.add(AppFolder(id = id, name = name, packageNames = pkgs))
                }
            }

            UserPreferences(
                mode = mode,
                iconStyle = iconStyle,
                favoritePackages = favs,
                hiddenPackages = hidden,
                lockedPackages = locked,
                folders = folders,
                gridColumns = gridCols,
                customAppNames = customNames,
                isAmoledBlack = isAmoled,
                isDynamicTheme = isDynamic,
                showNotificationBadges = showBadges,
                showClock = showClock,
                selectedIconPackPackage = iconPack,
                isOnboardingCompleted = true
            )
        } catch (_: Exception) {
            null
        }
    }

    fun shareBackup(context: Context, jsonString: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Posea Launcher Yedek")
            putExtra(Intent.EXTRA_TEXT, jsonString)
        }
        val chooser = Intent.createChooser(intent, "Posea Yedeğini Paylaş")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun copyToClipboard(context: Context, jsonString: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Posea Launcher Yedek", jsonString)
        clipboard.setPrimaryClip(clip)
    }
}
