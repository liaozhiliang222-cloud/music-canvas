package com.musiccanvas.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.musiccanvas.app.settings.MotionLevel

object ThemeIds {
    const val WARM_VINYL = "warm_vinyl"
    const val BLANK_COVER = "blank_cover"
    const val TAPE = "transparent_tape" // P1 占位，B1 不提供
}

data class ThemeInfo(
    val id: String,
    val displayName: String,
    val isPro: Boolean,
    val availableInBeta: Boolean,
)

val THEMES = listOf(
    ThemeInfo(ThemeIds.WARM_VINYL, "暖木黑胶", isPro = false, availableInBeta = true),
    ThemeInfo(ThemeIds.BLANK_COVER, "留白封面", isPro = true, availableInBeta = true),
)

@Composable
fun ArtworkArea(
    themeId: String,
    artwork: Bitmap?,
    isPlaying: Boolean,
    motionLevel: MotionLevel,
    accentColor: Int?,
    modifier: Modifier = Modifier,
) {
    when (themeId) {
        ThemeIds.BLANK_COVER -> BlankCoverArtwork(
            artwork = artwork,
            isPlaying = isPlaying,
            motionLevel = motionLevel,
            accentColor = accentColor,
            modifier = modifier.fillMaxSize(),
        )
        else -> WarmVinylArtwork(
            artwork = artwork,
            isPlaying = isPlaying,
            motionLevel = motionLevel,
            modifier = modifier,
        )
    }
}
