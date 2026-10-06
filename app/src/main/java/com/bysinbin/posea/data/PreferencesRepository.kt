package com.bysinbin.posea.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.model.LauncherMode
import com.bysinbin.posea.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "posea_preferences")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("launcher_mode")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FAVORITE_PACKAGES = stringSetPreferencesKey("favorite_packages")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val modeName = preferences[Keys.MODE] ?: LauncherMode.HYBRID.name
        val styleName = preferences[Keys.ICON_STYLE] ?: IconStyle.COLOR.name
        val isCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false
        val favorites = preferences[Keys.FAVORITE_PACKAGES] ?: emptySet()
        val showClock = preferences[Keys.SHOW_CLOCK] ?: true

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
            showClock = showClock
        )
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
}
