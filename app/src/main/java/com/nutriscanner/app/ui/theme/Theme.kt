package com.nutriscanner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScoreGreen = Color(0xFF2E7D32)
private val ScoreAmber = Color(0xFFF9A825)
private val ScoreRed = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    secondary = Color(0xFF00695C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFF4DB6AC),
)

/** Colors used to render score bands A-E; kept separate from the Material scheme since they're semantic, not brand colors. */
object ScoreColors {
    val Good = ScoreGreen
    val Mid = ScoreAmber
    val Poor = ScoreRed
}

@Composable
fun NutriScannerTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
