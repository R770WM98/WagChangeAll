package com.thisismine.myapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RiderBlueDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF002E73),
    primaryContainer = RiderBlueContainerDark,
    secondary = RiderGreenDark,
    secondaryContainer = RiderGreenContainerDark,
    tertiary = RiderAmberDark,
    tertiaryContainer = RiderAmberContainerDark,
    error = RiderErrorDark,
    surface = RiderSurfaceDark,
    surfaceVariant = RiderSurfaceVariantDark,
    outline = RiderOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = RiderBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = RiderBlueContainer,
    secondary = RiderGreen,
    secondaryContainer = RiderGreenContainer,
    tertiary = RiderAmber,
    tertiaryContainer = RiderAmberContainer,
    error = RiderError,
    surface = RiderSurfaceLight,
    surfaceVariant = RiderSurfaceVariantLight,
    outline = RiderOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep app colors consistent unless explicitly enabled.
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}