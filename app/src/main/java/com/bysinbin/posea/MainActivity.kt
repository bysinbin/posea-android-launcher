package com.bysinbin.posea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bysinbin.posea.theme.PoseaLauncherTheme
import com.bysinbin.posea.ui.main.MainScreen

import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bysinbin.posea.ui.components.IconCacheManager
import com.bysinbin.posea.ui.main.LauncherViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val preferences by viewModel.preferencesFlow.collectAsStateWithLifecycle()

            // İkon paketi senkronizasyonu
            IconCacheManager.activeIconPack = preferences.selectedIconPackPackage

            PoseaLauncherTheme(
                dynamicColor = preferences.isDynamicTheme,
                amoledBlack = preferences.isAmoledBlack
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (preferences.isAmoledBlack) Color.Black else Color.Transparent
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
