package dev.qtremors.eonslate.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Where accent colors come from. Eonslate's own preset is the default; wallpaper is opt-in (Android 12+). */
enum class AccentSource { WALLPAPER, PRESET, CUSTOM }

/**
 * Everything the theme needs, kept in the app's settings store. Defaults:
 * system light/dark, the Slate preset, haptics on, full motion.
 */
@Immutable
data class ThemeOptions(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val oled: Boolean = false,
    val accentSource: AccentSource = AccentSource.PRESET,
    val preset: ThemePreset = ThemePreset.SLATE,
    val customAccent: Color = BrandSeed,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    /** Hides the app from screenshots and the recent apps preview. */
    val blockScreenshots: Boolean = false
)

/** True when animations should be skipped or replaced by crossfades. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** False when the user turned haptics off in Settings. Read through rememberAppHaptics(). */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

val LocalSpacing = staticCompositionLocalOf { Spacing() }

@Composable
fun EonslateTheme(
    options: ThemeOptions = ThemeOptions(),
    content: @Composable () -> Unit
) {
    val dark = when (options.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val wallpaperAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = remember(options, dark, wallpaperAvailable) {
        val base = when {
            options.accentSource == AccentSource.WALLPAPER && wallpaperAvailable ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            options.accentSource == AccentSource.CUSTOM -> eonslateColorScheme(options.customAccent, dark)
            else -> eonslateColorScheme(options.preset.seed, dark)
        }
        if (dark && options.oled) base.toOled() else base
    }

    CompositionLocalProvider(
        LocalReduceMotion provides options.reduceMotion,
        LocalHapticsEnabled provides options.hapticsEnabled,
        LocalSpacing provides Spacing()
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            typography = EonslateTypography,
            shapes = EonslateShapes,
            motionScheme = if (options.reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
            content = content
        )
    }
}
