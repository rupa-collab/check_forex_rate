package com.checkrate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo800 = Color(0xFF283593)
private val LightColors = lightColorScheme(
    primary = Indigo800,
    onPrimary = Color.White,
    secondary = Indigo800,
    onSecondary = Color.White,
    tertiary = Indigo800,
    onTertiary = Color.White
)

@Composable
fun CheckRateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
