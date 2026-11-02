package com.komugi.workout.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

enum class ThemeMode(val id: String, val label: String) {
    SYSTEM("system", "跟隨系統"),
    LIGHT("light", "淺色"),
    DARK("dark", "深色");

    companion object {
        fun fromId(id: String?): ThemeMode =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

class ThemeManager(context: Context) {

    private val prefs = context.getSharedPreferences("komugi_theme", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        AppTheme.fromId(prefs.getString("theme_id", null))
    )
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _mode = MutableStateFlow(
        ThemeMode.fromId(prefs.getString("theme_mode", null))
    )
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setTheme(value: AppTheme) {
        prefs.edit().putString("theme_id", value.id).apply()
        _theme.value = value
    }

    fun setMode(value: ThemeMode) {
        prefs.edit().putString("theme_mode", value.id).apply()
        _mode.value = value
    }
}

private val ForestLight = lightColorScheme(
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

private val OceanLight = lightColorScheme(
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

private val SunsetLight = lightColorScheme(
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

private val ForestDark = darkColorScheme(
    primary = Color(0xFF9CD49F),
    onPrimary = Color(0xFF00390B),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB7F0B9),
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF243424),
    secondaryContainer = Color(0xFF3A4B39),
    onSecondaryContainer = Color(0xFFD5E8D0),
    tertiary = Color(0xFF9FCDD2),
    background = Color(0xFF11140F),
    onBackground = Color(0xFFE1E4DB),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFE1E4DB),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD)
)

private val OceanDark = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF00468B),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFB1CADD),
    onSecondary = Color(0xFF1C3344),
    secondaryContainer = Color(0xFF334A5C),
    onSecondaryContainer = Color(0xFFCDE5F8),
    tertiary = Color(0xFF82D3D8),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF)
)

private val SunsetDark = darkColorScheme(
    primary = Color(0xFFFFB695),
    onPrimary = Color(0xFF5F1600),
    primaryContainer = Color(0xFF8D2F0B),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE7BEB0),
    onSecondary = Color(0xFF442A20),
    secondaryContainer = Color(0xFF5D4035),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Color(0xFFD8C58D),
    background = Color(0xFF1A120E),
    onBackground = Color(0xFFF0E0D9),
    surface = Color(0xFF1A120E),
    onSurface = Color(0xFFF0E0D9),
    surfaceVariant = Color(0xFF53433C),
    onSurfaceVariant = Color(0xFFD8C2B8)
)

fun appColorScheme(appTheme: AppTheme, darkTheme: Boolean): ColorScheme =
    when (appTheme) {
        AppTheme.FOREST -> if (darkTheme) ForestDark else ForestLight
        AppTheme.OCEAN -> if (darkTheme) OceanDark else OceanLight
        AppTheme.SUNSET -> if (darkTheme) SunsetDark else SunsetLight
    }

/**
 * 全域 TopAppBar 配色：淺色用 primary，深色用 primaryContainer，
 * 讓頂部標題列與主題色一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun komugiTopAppBarColors(): TopAppBarColors {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val container = if (darkTheme) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }
    val content = if (darkTheme) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    return TopAppBarDefaults.topAppBarColors(
        containerColor = container,
        titleContentColor = content,
        navigationIconContentColor = content,
        actionIconContentColor = content
    )
}

@Composable
fun KomugiTheme(
    appTheme: AppTheme = AppTheme.FOREST,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = appColorScheme(appTheme, darkTheme),
        typography = Typography(),
        content = content
    )
}
