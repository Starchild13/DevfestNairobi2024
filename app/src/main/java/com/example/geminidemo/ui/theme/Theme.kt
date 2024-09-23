package com.example.geminidemo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF2C2C2C),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE1E1E1),
    onSurface = Color(0xFFE1E1E1)
)

// Light color scheme
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

// Custom colors for the chat bubbles
val ModelMessageColorDark = Color(0xFF3B5998) // Blue for model messages in dark mode
val UserMessageColorDark = Color(0xFF6E6E6E)  // Gray for user messages in dark mode

val ModelMessageColorLight = Color(0xFFD1E7FF) // Light blue for model messages in light mode
val UserMessageColorLight = Color(0xFFEFEFEF)  // Light gray for user messages in light mode

@Composable
fun GeminiChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Custom chat bubble colors based on the theme
    val modelMessageColor = if (darkTheme) ModelMessageColorDark else ModelMessageColorLight
    val userMessageColor = if (darkTheme) UserMessageColorDark else UserMessageColorLight

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            ChatThemeColors(
                modelMessageColor = modelMessageColor,
                userMessageColor = userMessageColor,
                content = content
            )
        }
    )
}

// Custom composable to provide chat bubble colors via CompositionLocal
@Composable
fun ChatThemeColors(
    modelMessageColor: Color,
    userMessageColor: Color,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalModelMessageColor provides modelMessageColor,
        LocalUserMessageColor provides userMessageColor,
        content = content
    )
}

// Provide chat bubble colors as CompositionLocal values
val LocalModelMessageColor = compositionLocalOf { Color.Unspecified }
val LocalUserMessageColor = compositionLocalOf { Color.Unspecified }

