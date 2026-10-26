package com.seniordict.app.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SeniorPrimary,
    onPrimary = SeniorSurface,
    primaryContainer = SeniorSurface,
    onPrimaryContainer = SeniorDark,
    background = SeniorBackground,
    onBackground = SeniorDark,
    surface = SeniorSurface,
    onSurface = SeniorDark
)

@Composable
fun SeniorDictTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = generateSequence(view.context) { ctx ->
                if (ctx is ContextWrapper) ctx.baseContext else null
            }.filterIsInstance<Activity>().firstOrNull()

            activity?.window?.let { window ->
                window.statusBarColor = SeniorBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = SeniorTypography,
        content = content
    )
}
