package com.musiccanvas.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.musiccanvas.app.settings.MotionLevel

/**
 * T02「留白封面」（PRD 7.2，R1 第二主题）：
 * 封面居中 + 纸感留白 + 低速环境光晕；提供完全静态模式。
 * 本主题为付费项：B1 中渲染但标记为 Pro。
 */
@Composable
fun BlankCoverArtwork(
    artwork: Bitmap?,
    isPlaying: Boolean,
    motionLevel: MotionLevel,
    accentColor: Int?,
    modifier: Modifier = Modifier,
) {
    // 低速呼吸光晕（0.05 Hz 量级），静态档不运行。
    var glowPhase by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying, motionLevel) {
        if (!isPlaying || motionLevel == MotionLevel.STATIC) {
            glowPhase = 0f
            return@LaunchedEffect
        }
        var last = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (last == 0L) { last = nanos; return@withFrameNanos }
                val dt = (nanos - last) / 1_000_000_000f
                last = nanos
                glowPhase = (glowPhase + dt * 0.12f) % 1f
            }
        }
    }

    val glowColor = accentColor?.let { Color(it) } ?: Amber
    val glowAlpha = if (motionLevel == MotionLevel.STATIC) 0.10f
    else 0.08f + 0.07f * kotlin.math.sin(glowPhase * 2 * Math.PI).toFloat().coerceAtLeast(0f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = glowAlpha),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.minDimension * 0.75f,
                ),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .shadow(24.dp, RoundedCornerShape(10.dp), clip = false)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkPanel),
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    val s = size.minDimension
                    drawCircle(
                        color = PaperWhite.copy(alpha = 0.25f),
                        radius = s * 0.30f,
                        center = center,
                    )
                    drawCircle(
                        color = Amber.copy(alpha = 0.5f),
                        radius = s * 0.18f,
                        center = center,
                    )
                    drawCircle(color = WarmBlack, radius = s * 0.02f, center = center)
                }
            }
        }
    }
}
