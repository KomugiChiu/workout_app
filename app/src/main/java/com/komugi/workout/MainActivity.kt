package com.komugi.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import com.komugi.workout.ui.AppRoot
import com.komugi.workout.ui.theme.KomugiTheme
import com.komugi.workout.ui.theme.ThemeMode
import com.komugi.workout.ui.theme.appColorScheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KomugiApp
        val themeManager = app.themeManager

        setContent {
            val theme by themeManager.theme.collectAsState()
            val mode by themeManager.mode.collectAsState()

            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SideEffect {
                val scheme = appColorScheme(theme, darkTheme)
                val barColor = if (darkTheme) {
                    scheme.primaryContainer
                } else {
                    scheme.primary
                }
                window.statusBarColor = barColor.toArgb()
                window.navigationBarColor = barColor.toArgb()
            }

            KomugiTheme(appTheme = theme, darkTheme = darkTheme) {
                AppRoot(
                    repository = app.repository,
                    themeManager = themeManager
                )
            }
        }
    }
}
