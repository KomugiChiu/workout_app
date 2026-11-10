package com.komugi.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.komugi.workout.ui.AppRoot
import com.komugi.workout.ui.theme.KomugiTheme
import com.komugi.workout.ui.theme.ThemeMode
import com.komugi.workout.ui.theme.appColorScheme

class MainActivity : ComponentActivity() {

    /** Android 13+ 通知權限：組間休息倒數要在狀態列顯示進度需要它。 */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as KomugiApp
        val themeManager = app.themeManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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
