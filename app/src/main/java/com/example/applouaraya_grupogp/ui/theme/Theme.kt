package com.example.applouaraya_grupogp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.applouaraya_grupogp.ui.design.LouColors

private val LouScheme = lightColorScheme(
    primary = LouColors.Terracota,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = LouColors.Arena,
    onPrimaryContainer = LouColors.Cafe,
    secondary = LouColors.Cafe,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = LouColors.Fondo,
    onBackground = LouColors.Cafe,
    surface = LouColors.Superficie,
    onSurface = LouColors.Cafe,
    outline = LouColors.TextoSecundario
)

/** No usar colores dinámicos: esta marca necesita conservar sus tonos terracota. */
@Composable
fun AppLouAraya_grupoGPTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = LouScheme, typography = Typography, content = content)
}
