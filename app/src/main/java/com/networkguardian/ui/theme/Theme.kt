package com.networkguardian.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = GuardianTeal80,
    onPrimary = GuardianNavy10,
    secondary = GuardianAmber,
    background = SurfaceDark,
    surface = GuardianNavy20,
    error = GuardianRed
)

private val LightColors = lightColorScheme(
    primary = GuardianTeal40,
    onPrimary = Color_White,
    secondary = GuardianAmber,
    background = SurfaceLight,
    surface = Color_White,
    error = GuardianRed
)

private val Color_White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)

@Composable
fun NetworkGuardianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GuardianTypography,
        content = content
    )
}
