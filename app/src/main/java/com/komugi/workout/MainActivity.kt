package com.komugi.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import com.komugi.workout.ui.AppRoot
import com.komugi.workout.ui.theme.KomugiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KomugiApp
        val themeManager = app.themeManager

        setContent {
            val theme by themeManager.theme.collectAsState()

            SideEffect {
                window.statusBarColor = theme.previewColor.toArgb()
            }

            KomugiTheme(appTheme = theme) {
                AppRoot(
                    repository = app.repository,
                    themeManager = themeManager
                )
            }
        }
    }
}
