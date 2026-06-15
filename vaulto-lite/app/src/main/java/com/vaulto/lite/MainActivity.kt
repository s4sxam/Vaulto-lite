package com.vaulto.lite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vaulto.lite.data.settings.ThemeMode
import com.vaulto.lite.ui.MainViewModelFactory
import com.vaulto.lite.ui.navigation.VaultoNavHost
import com.vaulto.lite.ui.theme.VaultoLiteTheme
import com.vaulto.lite.ui.util.collectAsStateWithLifecycleCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as VaultoApplication

        setContent {
            val viewModel = viewModel(
                factory = MainViewModelFactory(app.repository, app.settingsRepository)
            )

            val themeMode by viewModel.themeMode.collectAsStateWithLifecycleCompat()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            VaultoLiteTheme(darkTheme = darkTheme) {
                VaultoNavHost(viewModel = viewModel)
            }
        }
    }
}
