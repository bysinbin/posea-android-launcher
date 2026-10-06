package com.bysinbin.posea.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bysinbin.posea.model.AppFolder
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.model.LauncherMode
import com.bysinbin.posea.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "posea_preferences")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("launcher_mode")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FAVORITE_PACKAGES = stringSetPreferencesKey("favorite_packages")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val PINNED_WIDGET_IDS = stringPreferencesKey("pinned_widget_ids")
        val HOME_WIDGET_IDS = stringPreferencesKey("home_widget_ids")
        val FOLDERS_JSON = stringPreferencesKey("folders_json")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val modeName = preferences[Keys.MODE] ?: LauncherMode.HYBRID.name
        val styleName = preferences[Keys.ICON_STYLE] ?: IconStyle.COLOR.name
        val isCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false
        val favorites = preferences[Keys.FAVORITE_PACKAGES] ?: emptySet()
        val hidden = preferences[Keys.HIDDEN_PACKAGES] ?: emptySet()
        val showClock = preferences[Keys.SHOW_CLOCK] ?: true
        val gridCols = preferences[Keys.GRID_COLUMNS] ?: 4

        val widgetIdsRaw = preferences[Keys.PINNED_WIDGET_IDS] ?: ""
        val widgetIds = if (widgetIdsRaw.isBlank()) emptyList() else widgetIdsRaw.split(",").mapNotNull { it.toIntOrNull() }

        val homeWidgetIdsRaw = preferences[Keys.HOME_WIDGET_IDS] ?: ""
        val homeWidgetIds = if (homeWidgetIdsRaw.isBlank()) emptyList() else homeWidgetIdsRaw.split(",").mapNotNull { it.toIntOrNull() }

        val foldersRaw = preferences[Keys.FOLDERS_JSON] ?: ""
        val folders = parseFolders(foldersRaw)

        UserPreferences(
            mode = try {
                LauncherMode.valueOf(modeName)
            } catch (e: Exception) {
                LauncherMode.HYBRID
            },
            iconStyle = try {
                IconStyle.valueOf(styleName)
            } catch (e: Exception) {
                IconStyle.COLOR
            },
            isOnboardingCompleted = isCompleted,
            favoritePackages = favorites,
            hiddenPackages = hidden,
            folders = folders,
            pinnedWidgetIds = widgetIds,
            homeWidgetIds = homeWidgetIds,
            showClock = showClock,
            gridColumns = gridCols.coerceIn(3, 6)
        )
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.GRID_COLUMNS] = columns.coerceIn(3, 6)
        }
    }

    suspend fun toggleHideApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[Keys.HIDDEN_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            preferences[Keys.HIDDEN_PACKAGES] = current
        }
    }

    suspend fun setHiddenPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HIDDEN_PACKAGES] = packages
        }
    }

    suspend fun addPinnedWidget(widgetId: Int) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[Keys.PINNED_WIDGET_IDS] ?: ""
            val list = if (currentRaw.isBlank()) mutableListOf() else currentRaw.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
            if (!list.contains(widgetId)) {
                list.add(widgetId)
                preferences[Keys.PINNED_WIDGET_IDS] = list.joinToString(",")
            }
        }
    }

    suspend fun removePinnedWidget(widgetId: Int) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[Keys.PINNED_WIDGET_IDS] ?: ""
            val list = if (currentRaw.isBlank()) mutableListOf() else currentRaw.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
            list.remove(widgetId)
            preferences[Keys.PINNED_WIDGET_IDS] = list.joinToString(",")
        }
    }

    suspend fun addHomeWidget(widgetId: Int) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[Keys.HOME_WIDGET_IDS] ?: ""
            val list = if (currentRaw.isBlank()) mutableListOf() else currentRaw.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
            if (!list.contains(widgetId)) {
                list.add(widgetId)
                preferences[Keys.HOME_WIDGET_IDS] = list.joinToString(",")
            }
        }
    }

    suspend fun removeHomeWidget(widgetId: Int) {
        context.dataStore.edit { preferences ->
            val currentRaw = preferences[Keys.HOME_WIDGET_IDS] ?: ""
            val list = if (currentRaw.isBlank()) mutableListOf() else currentRaw.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
            list.remove(widgetId)
            preferences[Keys.HOME_WIDGET_IDS] = list.joinToString(",")
        }
    }

    // Klasör / Grup İşlemleri
    suspend fun createFolder(name: String, packageNames: List<String>): String {
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { preferences ->
            val current = parseFolders(preferences[Keys.FOLDERS_JSON] ?: "").toMutableList()
            current.add(AppFolder(id = newId, name = name, packageNames = packageNames.distinct()))
            preferences[Keys.FOLDERS_JSON] = serializeFolders(current)
        }
        return newId
    }

    suspend fun deleteFolder(folderId: String) {
        context.dataStore.edit { preferences ->
            val current = parseFolders(preferences[Keys.FOLDERS_JSON] ?: "").filter { it.id != folderId }
            preferences[Keys.FOLDERS_JSON] = serializeFolders(current)
        }
    }

    suspend fun renameFolder(folderId: String, newName: String) {
        context.dataStore.edit { preferences ->
            val current = parseFolders(preferences[Keys.FOLDERS_JSON] ?: "").map {
                if (it.id == folderId) it.copy(name = newName) else it
            }
            preferences[Keys.FOLDERS_JSON] = serializeFolders(current)
        }
    }

    suspend fun addAppToFolder(folderId: String, packageName: String) {
        context.dataStore.edit { preferences ->
            val current = parseFolders(preferences[Keys.FOLDERS_JSON] ?: "").map {
                if (it.id == folderId && !it.packageNames.contains(packageName)) {
                    it.copy(packageNames = it.packageNames + packageName)
                } else it
            }
            preferences[Keys.FOLDERS_JSON] = serializeFolders(current)
        }
    }

    suspend fun removeAppFromFolder(folderId: String, packageName: String) {
        context.dataStore.edit { preferences ->
            val current = parseFolders(preferences[Keys.FOLDERS_JSON] ?: "").map {
                if (it.id == folderId) {
                    it.copy(packageNames = it.packageNames.filter { p -> p != packageName })
                } else it
            }
            preferences[Keys.FOLDERS_JSON] = serializeFolders(current)
        }
    }

    suspend fun setLauncherMode(mode: LauncherMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MODE] = mode.name
        }
    }

    suspend fun setIconStyle(style: IconStyle) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ICON_STYLE] = style.name
        }
    }

    suspend fun toggleFavorite(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[Keys.FAVORITE_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            preferences[Keys.FAVORITE_PACKAGES] = current
        }
    }

    suspend fun setFavorites(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[Keys.FAVORITE_PACKAGES] = packages
        }
    }

    suspend fun completeOnboarding(
        mode: LauncherMode,
        iconStyle: IconStyle,
        favorites: Set<String>
    ) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MODE] = mode.name
            preferences[Keys.ICON_STYLE] = iconStyle.name
            preferences[Keys.FAVORITE_PACKAGES] = favorites
            preferences[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun resetOnboarding() {
        context.dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = false
        }
    }

    companion object {
        fun serializeFolders(folders: List<AppFolder>): String {
            val array = JSONArray()
            for (folder in folders) {
                val obj = JSONObject()
                obj.put("id", folder.id)
                obj.put("name", folder.name)
                val pkgs = JSONArray()
                folder.packageNames.forEach { pkgs.put(it) }
                obj.put("packages", pkgs)
                array.put(obj)
            }
            return array.toString()
        }

        fun parseFolders(raw: String): List<AppFolder> {
            if (raw.isBlank()) return emptyList()
            return try {
                val array = JSONArray(raw)
                val list = mutableListOf<AppFolder>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val pkgsArray = obj.optJSONArray("packages")
                    val pkgs = mutableListOf<String>()
                    if (pkgsArray != null) {
                        for (j in 0 until pkgsArray.length()) {
                            pkgs.add(pkgsArray.getString(j))
                        }
                    }
                    list.add(AppFolder(id = id, name = name, packageNames = pkgs))
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
