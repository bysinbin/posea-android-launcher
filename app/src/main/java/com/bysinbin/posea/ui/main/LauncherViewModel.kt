package com.bysinbin.posea.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bysinbin.posea.data.AppsRepository
import com.bysinbin.posea.data.PreferencesRepository
import com.bysinbin.posea.data.system.LauncherWidgetManager
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.model.LauncherMode
import com.bysinbin.posea.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import androidx.compose.runtime.Immutable
import com.bysinbin.posea.ui.components.IconCacheManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class LauncherUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val allApps: List<AppModel> = emptyList(),
    val favoriteApps: List<AppModel> = emptyList(),
    val hiddenApps: List<AppModel> = emptyList(),
    val recentApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val filteredApps: List<AppModel> = emptyList(),
    val isDrawerOpen: Boolean = false,
    val isSettingsOpen: Boolean = false
)

class LauncherViewModel @JvmOverloads constructor(
    application: Application,
    private val appsRepository: AppsRepository = AppsRepository(application),
    private val preferencesRepository: PreferencesRepository = PreferencesRepository(application),
    val widgetManager: LauncherWidgetManager = LauncherWidgetManager(application)
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    private val _isDrawerOpen = MutableStateFlow(false)
    private val _isSettingsOpen = MutableStateFlow(false)

    init {
        // İkonları arka plan iş parçacığında önceden belleğe alarak 120 FPS akıcılık sağla
        viewModelScope.launch {
            appsRepository.installedAppsFlow.collect { apps ->
                IconCacheManager.preload(application, apps)
            }
        }
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        appsRepository.installedAppsFlow,
        preferencesRepository.userPreferencesFlow,
        _searchQuery,
        _isDrawerOpen,
        _isSettingsOpen
    ) { apps, preferences, query, isDrawerOpen, isSettingsOpen ->
        // Özel isimleri ve favori durumlarını uygula
        val appsProcessed = apps.map { app ->
            val customLabel = preferences.customAppNames[app.packageName]
            val finalLabel = if (!customLabel.isNullOrBlank()) customLabel else app.label
            app.copy(
                label = finalLabel,
                isFavorite = preferences.favoritePackages.contains(app.packageName)
            )
        }.sortedBy { it.label.lowercase() }

        // Gizlenen uygulamaları ayır
        val visibleApps = appsProcessed.filter { !preferences.hiddenPackages.contains(it.packageName) }
        val hiddenApps = appsProcessed.filter { preferences.hiddenPackages.contains(it.packageName) }

        // Favori uygulamaları filtrele
        val favoriteApps = visibleApps.filter { it.isFavorite }

        // Son açılan uygulamalar
        val appMap = visibleApps.associateBy { it.packageName }
        val recentApps = preferences.recentAppPackages.mapNotNull { appMap[it] }.take(5)

        // Arama filtresi
        val filteredApps = if (query.isBlank()) {
            visibleApps
        } else {
            visibleApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }

        LauncherUiState(
            isLoading = false,
            preferences = preferences,
            allApps = visibleApps,
            favoriteApps = favoriteApps,
            hiddenApps = hiddenApps,
            recentApps = recentApps,
            searchQuery = query,
            filteredApps = filteredApps,
            isDrawerOpen = isDrawerOpen,
            isSettingsOpen = isSettingsOpen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LauncherUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setDrawerOpen(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
        if (!isOpen) {
            _searchQuery.value = ""
        }
    }

    fun setSettingsOpen(isOpen: Boolean) {
        _isSettingsOpen.value = isOpen
    }

    fun launchApp(app: AppModel) {
        viewModelScope.launch {
            preferencesRepository.addRecentApp(app.packageName)
            appsRepository.launchApp(app)
        }
    }

    fun openAppInfo(app: AppModel) {
        appsRepository.openAppInfo(app.packageName)
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(packageName)
        }
    }

    fun toggleHideApp(packageName: String) {
        viewModelScope.launch {
            preferencesRepository.toggleHideApp(packageName)
        }
    }

    fun setCustomAppName(packageName: String, customName: String) {
        viewModelScope.launch {
            preferencesRepository.setCustomAppName(packageName, customName)
        }
    }

    fun resetCustomAppName(packageName: String) {
        viewModelScope.launch {
            preferencesRepository.resetCustomAppName(packageName)
        }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAmoledBlack(enabled)
        }
    }

    fun setDynamicTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicTheme(enabled)
        }
    }

    fun setIconPackPackage(packageName: String?) {
        viewModelScope.launch {
            preferencesRepository.setIconPackPackage(packageName)
        }
    }

    fun setGridColumns(count: Int) {
        viewModelScope.launch {
            preferencesRepository.setGridColumns(count)
        }
    }

    fun setLauncherMode(mode: LauncherMode) {
        viewModelScope.launch {
            preferencesRepository.setLauncherMode(mode)
        }
    }

    fun setIconStyle(style: IconStyle) {
        viewModelScope.launch {
            preferencesRepository.setIconStyle(style)
        }
    }

    fun completeOnboarding(mode: LauncherMode, iconStyle: IconStyle, favorites: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.completeOnboarding(mode, iconStyle, favorites)
        }
    }

    fun addPinnedWidget(widgetId: Int) {
        viewModelScope.launch {
            preferencesRepository.addPinnedWidget(widgetId)
        }
    }

    fun removePinnedWidget(widgetId: Int) {
        viewModelScope.launch {
            widgetManager.deleteAppWidgetId(widgetId)
            preferencesRepository.removePinnedWidget(widgetId)
        }
    }

    fun addHomeWidget(widgetId: Int) {
        viewModelScope.launch {
            preferencesRepository.addHomeWidget(widgetId)
        }
    }

    fun removeHomeWidget(widgetId: Int) {
        viewModelScope.launch {
            widgetManager.deleteAppWidgetId(widgetId)
            preferencesRepository.removeHomeWidget(widgetId)
        }
    }

    // Klasör Yöneticisi
    fun createFolder(name: String, packageNames: List<String>) {
        viewModelScope.launch {
            preferencesRepository.createFolder(name, packageNames)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            preferencesRepository.deleteFolder(folderId)
        }
    }

    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            preferencesRepository.renameFolder(folderId, newName)
        }
    }

    fun addAppToFolder(folderId: String, packageName: String) {
        viewModelScope.launch {
            preferencesRepository.addAppToFolder(folderId, packageName)
        }
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        viewModelScope.launch {
            preferencesRepository.removeAppFromFolder(folderId, packageName)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.resetOnboarding()
        }
    }

    fun setShowNotificationBadges(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowNotificationBadges(enabled)
        }
    }

    fun toggleLockApp(packageName: String) {
        viewModelScope.launch {
            preferencesRepository.toggleLockApp(packageName)
        }
    }

    fun setLockedPackages(packages: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setLockedPackages(packages)
        }
    }

    fun restorePreferences(newPrefs: UserPreferences) {
        viewModelScope.launch {
            preferencesRepository.restorePreferences(newPrefs)
        }
    }
}
