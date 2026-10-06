package com.bysinbin.posea.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bysinbin.posea.data.AppsRepository
import com.bysinbin.posea.data.PreferencesRepository
import com.bysinbin.posea.model.AppModel
import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.model.LauncherMode
import com.bysinbin.posea.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LauncherUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val allApps: List<AppModel> = emptyList(),
    val favoriteApps: List<AppModel> = emptyList(),
    val searchQuery: String = "",
    val filteredApps: List<AppModel> = emptyList(),
    val isDrawerOpen: Boolean = false,
    val isSettingsOpen: Boolean = false
)

class LauncherViewModel(
    application: Application,
    private val appsRepository: AppsRepository = AppsRepository(application),
    private val preferencesRepository: PreferencesRepository = PreferencesRepository(application)
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    private val _isDrawerOpen = MutableStateFlow(false)
    private val _isSettingsOpen = MutableStateFlow(false)

    val uiState: StateFlow<LauncherUiState> = combine(
        appsRepository.installedAppsFlow,
        preferencesRepository.userPreferencesFlow,
        _searchQuery,
        _isDrawerOpen,
        _isSettingsOpen
    ) { apps, preferences, query, isDrawerOpen, isSettingsOpen ->
        // Favori durumlarını uygula
        val appsWithFavorites = apps.map { app ->
            app.copy(isFavorite = preferences.favoritePackages.contains(app.packageName))
        }

        // Favori uygulamaları filtrele
        val favoriteApps = appsWithFavorites.filter { it.isFavorite }

        // Arama filtresi
        val filteredApps = if (query.isBlank()) {
            appsWithFavorites
        } else {
            appsWithFavorites.filter {
                it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }

        LauncherUiState(
            isLoading = false,
            preferences = preferences,
            allApps = appsWithFavorites,
            favoriteApps = favoriteApps,
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

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.resetOnboarding()
        }
    }
}
