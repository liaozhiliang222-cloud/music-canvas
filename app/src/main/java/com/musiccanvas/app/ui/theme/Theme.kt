package com.musiccanvas.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val WarmBlack = Color(0xFF151715)
val PaperWhite = Color(0xFFF2EBDD)
val Amber = Color(0xFFC99454)
val Sage = Color(0xFF8C9A8F)
val DarkPanel = Color(0xFF221F1A)
val GrooveColor = Color(0xFF3A362E)

private val scheme = darkColorScheme(
    primary = Amber,
    onPrimary = WarmBlack,
    secondary = Sage,
    onSecondary = WarmBlack,
    background = WarmBlack,
    onBackground = PaperWhite,
    surface = DarkPanel,
    onSurface = PaperWhite,
    surfaceVariant = Color(0xFF2B2820),
    onSurfaceVariant = Sage,
    error = Color(0xFFE0A08A),
)

@Composable
fun MusicCanvasTheme(content: @Composable () -> Unit) {
    // 产品本身是深色桌面摆件，固定深色方案。
    MaterialTheme(colorScheme = scheme, content = content)
}
