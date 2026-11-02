package com.komugi.workout.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme(
    val id: String,
    val label: String,
    val description: String,
    val previewColor: Color
) {
    FOREST("forest", "森林綠", "沉穩、專注", Color(0xFF2E7D32)),
    OCEAN("ocean", "海洋藍", "清爽、冷靜", Color(0xFF1565C0)),
    SUNSET("sunset", "暖陽橘", "活力、溫暖", Color(0xFFC2410C));

    companion object {
        fun fromId(id: String?): AppTheme =
            entries.firstOrNull { it.id == id } ?: FOREST
    }
}

class ThemeManager(context: Context) {

    private val prefs = context.getSharedPreferences("komugi_theme", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        AppTheme.fromId(prefs.getString("theme_id", null))
    )
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun setTheme(value: AppTheme) {
        prefs.edit().putString("theme_id", value.id).apply()
        _theme.value = value
    }
}

private val ForestColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F0B9),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Color(0xFF52634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8D0),
    onSecondaryContainer = Color(0xFF101F10),
    tertiary = Color(0xFF38656A),
    background = Color(0xFFFBFDF7),
    onBackground = Color(0xFF191D17),
    surface = Color(0xFFFBFDF7),
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940)
)

private val OceanColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF4A6274),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE5F8),
    onSecondaryContainer = Color(0xFF051F2E),
    tertiary = Color(0xFF00696E),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E)
)

private val SunsetColors = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF77574A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCC),
    onSecondaryContainer = Color(0xFF2C150C),
    tertiary = Color(0xFF6C5D2F),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A16),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A16),
    surfaceVariant = Color(0xFFF5DED5),
    onSurfaceVariant = Color(0xFF53433C)
)

@Composable
fun KomugiTheme(
    appTheme: AppTheme = AppTheme.FOREST,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.FOREST -> ForestColors
        AppTheme.OCEAN -> OceanColors
        AppTheme.SUNSET -> SunsetColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
