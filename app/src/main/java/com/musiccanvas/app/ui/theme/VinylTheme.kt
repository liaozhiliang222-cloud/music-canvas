package com.musiccanvas.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.roundToInt
import com.musiccanvas.app.settings.MotionLevel

/**
 * T01「暖木黑胶」（PRD 7.1）：
 * - 唱片按固定视觉速度旋转，与 BPM/真实转速无关；
 * - 暂停时减速停止（400–800ms），唱臂停泊；
 * - 缺封面用原创抽象标签；不宣称任何频谱/节拍含义。
 */
@Composable
fun WarmVinylArtwork(
    artwork: Bitmap?,
    isPlaying: Boolean,
    motionLevel: MotionLevel,
    modifier: Modifier = Modifier,
) {
    // 动效角度状态机：播放→匀速旋转；暂停→400–800ms 减速停止；静态→不转。
    var angle by remember { mutableStateOf(0f) }
    var velocity by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying, motionLevel) {
        if (motionLevel == MotionLevel.STATIC) {
            velocity = 0f
            return@LaunchedEffect
        }
        val targetRpm = 45f / 60f * 360f // 视觉速度：约 45rpm，纯装饰
        var lastNanos = 0L
        var stopping = !isPlaying && velocity > 0f
        val decelDurationNanos = 600_000_000L // 600ms 减速
        var stopStartNanos = 0L
        var stopStartVelocity = velocity

        while (true) {
            withFrameNanos { nanos ->
                if (lastNanos == 0L) {
                    lastNanos = nanos
                    stopStartNanos = nanos
                    return@withFrameNanos
                }
                val dt = (nanos - lastNanos) / 1_000_000_000f
                lastNanos = nanos
                when {
                    isPlaying && motionLevel != MotionLevel.STATIC -> {
                        velocity = targetRpm
                        stopping = false
                    }
                    stopping -> {
                        val t = ((nanos - stopStartNanos).toFloat() / decelDurationNanos)
                            .coerceIn(0f, 1f)
                        velocity = stopStartVelocity * (1f - t)
                        if (t >= 1f) {
                            velocity = 0f
                            stopping = false
                        }
                    }
                    !isPlaying && velocity > 0f && !stopping -> {
                        stopping = true
                        stopStartNanos = nanos
                        stopStartVelocity = velocity
                    }
                }
                angle = (angle + velocity * dt) % 360f
            }
            if (!isPlaying && velocity == 0f) break
        }
    }

    Box(modifier = modifier.aspectRatio(1f).clipToBounds()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)
            val discRadius = s * 0.46f
            val labelRadius = s * 0.16f
            val tonearmPivot = Offset(size.width * 0.82f, size.height * 0.16f)

            // 木质面板底纹（抽象同心环，非仿制品）
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF3D2F22), Color(0xFF241D16)),
                    center = center, radius = s * 0.7f,
                ),
                radius = s * 0.62f,
                center = center,
            )
            repeat(5) { i ->
                drawCircle(
                    color = Color(0xFF4A3B2A).copy(alpha = 0.10f),
                    radius = s * (0.5f + i * 0.045f),
                    center = center,
                    style = Stroke(width = s * 0.004f),
                )
            }

            // 唱片
            rotate(angle, pivot = center) {
                drawCircle(color = Color(0xFF191713), radius = discRadius, center = center)
                val ringCount = 10
                for (i in 0 until ringCount) {
                    drawCircle(
                        color = Color(0xFF35312A).copy(alpha = 0.6f),
                        radius = discRadius * (0.42f + i * 0.055f),
                        center = center,
                        style = Stroke(width = s * 0.0016f),
                    )
                }
                // 高光
                drawArc(
                    color = Color.White.copy(alpha = 0.05f),
                    startAngle = 305f, sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(center.x - discRadius, center.y - discRadius),
                    size = Size(discRadius * 2, discRadius * 2),
                    style = Stroke(width = s * 0.018f),
                )
            }

            // 标签区
            drawCircle(color = Amber, radius = labelRadius, center = center)
            drawCircle(color = Color(0xFFB07F45), radius = labelRadius * 0.55f, center = center)
        }

        // 封面贴标签中心（裁剪为圆，跟随唱片视觉但不同步自转，避免眩晕）
        if (artwork != null) {
            VinylLabelImage(artwork = artwork)
        } else {
            PlaceholderLabel()
        }

        // 唱臂
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.minDimension
            val pivot = Offset(size.width * 0.84f, size.height * 0.15f)
            val armLength = s * 0.62f
            // 播放时唱臂落在唱片上（约 30°），暂停时回停泊位（约 -5°）
            val armAngle = if (isPlaying) 28f else -6f
            drawCircle(color = Color(0xFF6B5B45), radius = s * 0.032f, center = pivot)
            rotate(degrees = armAngle, pivot = pivot) {
                drawLine(
                    color = Color(0xFF8A7A5E),
                    start = pivot,
                    end = Offset(pivot.x, pivot.y + armLength),
                    strokeWidth = s * 0.010f,
                )
                drawCircle(
                    color = Color(0xFF544838),
                    radius = s * 0.020f,
                    center = Offset(pivot.x, pivot.y + armLength),
                )
            }
        }
    }
}

@Composable
private fun VinylLabelImage(artwork: Bitmap) {
    val bmp = remember(artwork) { artwork.asImageBitmap() }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val s = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val labelRadius = s * 0.155f
        // 封面裁剪进圆形标签
        clipRect(0f, 0f, size.width, size.height) {
            val path = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center.x - labelRadius, center.y - labelRadius,
                        center.x + labelRadius, center.y + labelRadius,
                    )
                )
            }
            clipPath(path) {
                val scale = maxOf(
                    (labelRadius * 2) / bmp.width,
                    (labelRadius * 2) / bmp.height,
                )
                val w = (bmp.width * scale).roundToInt()
                val h = (bmp.height * scale).roundToInt()
                drawImage(
                    image = bmp,
                    dstOffset = IntOffset(
                        (center.x - w / 2f).roundToInt(),
                        (center.y - h / 2f).roundToInt(),
                    ),
                    dstSize = IntSize(w, h),
                )
            }
        }
        drawCircle(
            color = WarmBlack, radius = s * 0.008f, center = center,
        )
    }
}

@Composable
private fun PlaceholderLabel() {
    // 原创抽象占位标签：三个同心弧，不以外网图片补齐（PRD 7.1）。
    Canvas(modifier = Modifier.fillMaxSize()) {
        val s = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = s * 0.10f
        listOf(1f, 0.72f, 0.45f).forEachIndexed { i, f ->
            drawArc(
                color = listOf(PaperWhite, Amber, Sage)[i],
                startAngle = 60f * i, sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(center.x - r * f, center.y - r * f),
                size = Size(r * f * 2, r * f * 2),
                style = Stroke(width = s * 0.010f),
            )
        }
        drawCircle(color = WarmBlack, radius = s * 0.008f, center = center)
    }
}
