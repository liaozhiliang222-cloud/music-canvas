package com.musiccanvas.app.desk

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musiccanvas.app.MusicCanvasApp
import com.musiccanvas.app.R
import com.musiccanvas.app.media.AppUiState
import com.musiccanvas.app.media.PlayerCommand
import com.musiccanvas.app.settings.MotionLevel
import com.musiccanvas.app.ui.AppViewModel
import com.musiccanvas.app.ui.theme.ArtworkArea
import com.musiccanvas.app.ui.theme.Amber
import com.musiccanvas.app.ui.theme.MusicCanvasTheme
import com.musiccanvas.app.ui.theme.PaperWhite
import com.musiccanvas.app.ui.theme.Sage
import com.musiccanvas.app.ui.theme.WarmBlack
import kotlinx.coroutines.delay

/**
 * 桌面展示模式（PRD 12）：
 * - 用户主动开始；FLAG_KEEP_SCREEN_ON 仅前台生效；
 * - 默认 15/30/60 分钟到时结束；控件 5 秒自动隐藏；
 * - 暂停超 60s 停持续动效；后台/锁屏停止渲染计时。
 */
class DeskActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MusicCanvasTheme {
                val vm: AppViewModel = viewModel { AppViewModel(applicationContext) }
                DeskScreen(viewModel = vm, onFinish = { finish() })
            }
        }
    }

    override fun onDestroy() {
        // MC-041/DisplayFinished：取消保持亮屏，恢复正常屏幕策略。
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}

@Composable
private fun DeskScreen(viewModel: AppViewModel, onFinish: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    val snap = ui.snapshot
    val inDemo = ui.inDemoMode || !snap.permissionGranted

    var controlsVisible by remember { mutableStateOf(true) }
    var remainingMs by remember { mutableLongStateOf(ui.prefs.deskTimerMinutes * 60_000L) }
    var pausedSinceMs by remember { mutableLongStateOf(0L) }
    var finished by remember { mutableStateOf(false) }

    val playing = if (inDemo) ui.demoPlaying else snap.nowPlaying.isPlaying
    val title = if (inDemo) ui.demoTrack.title else snap.nowPlaying.title
    val artist = if (inDemo) ui.demoTrack.artist else snap.nowPlaying.artist
    val artwork = if (inDemo) null else snap.artwork.bitmap
    val isLandscape = LocalConfiguration.current.screenWidthDp >
        LocalConfiguration.current.screenHeightDp

    // 首次使用提示（PRD 12.2）
    LaunchedEffect(Unit) {
        if (!ui.prefs.deskHintShown) viewModel.setDeskHintShown()
    }

    // 倒计时：到时不锁屏，只结束展示会话（MC-041）。
    LaunchedEffect(remainingMs, finished) {
        while (!finished && remainingMs > 0) {
            delay(1000)
            remainingMs -= 1000
        }
        if (!finished) {
            finished = true
        }
    }

    // 控件自动隐藏（MC-042）。
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000)
            controlsVisible = false
        }
    }

    // 暂停超 60s → 静态化；继续暂停则计时结束后不恢复亮屏（MC-044）。
    LaunchedEffect(playing) {
        if (!playing) {
            pausedSinceMs = System.currentTimeMillis()
        } else {
            pausedSinceMs = 0L
        }
    }
    val motionLevel = when {
        !playing && pausedSinceMs > 0 &&
            System.currentTimeMillis() - pausedSinceMs > 60_000 -> MotionLevel.STATIC
        else -> ui.prefs.motionLevel
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBlack)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsVisible = !controlsVisible },
    ) {
        if (isLandscape) {
            // 横屏：唱盘居左、信息居右（PRD 7.1 横屏规格）
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(0.55f).padding(24.dp)) {
                    ArtworkArea(
                        themeId = ui.prefs.themeId,
                        artwork = artwork,
                        isPlaying = playing,
                        motionLevel = motionLevel,
                        accentColor = snap.artwork.accentColor,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    Modifier.weight(0.45f).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (!ui.prefs.hideTrackInfo) {
                        Text(
                            title ?: stringResource(R.string.track_info_unavailable),
                            style = MaterialTheme.typography.headlineMedium,
                            color = PaperWhite,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            artist ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Sage, maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    DeskControls(
                        visible = controlsVisible,
                        inDemo = inDemo,
                        snap = snap,
                        viewModel = viewModel,
                        playing = playing,
                        onFinish = onFinish,
                        remainingMs = remainingMs,
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(0.62f).padding(24.dp)) {
                    ArtworkArea(
                        themeId = ui.prefs.themeId,
                        artwork = artwork,
                        isPlaying = playing,
                        motionLevel = motionLevel,
                        accentColor = snap.artwork.accentColor,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (!ui.prefs.hideTrackInfo) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            title ?: stringResource(R.string.track_info_unavailable),
                            style = MaterialTheme.typography.titleLarge,
                            color = PaperWhite, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(artist ?: "", color = Sage, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.weight(0.10f))
                DeskControls(
                    visible = controlsVisible,
                    inDemo = inDemo,
                    snap = snap,
                    viewModel = viewModel,
                    playing = playing,
                    onFinish = onFinish,
                    remainingMs = remainingMs,
                )
            }
        }

        // 结束遮罩
        if (finished) {
            Surface(
                color = WarmBlack.copy(alpha = 0.92f),
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.desk_finished),
                        style = MaterialTheme.typography.titleMedium,
                        color = PaperWhite,
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Amber,
                        modifier = Modifier.clickable { onFinish() },
                    ) {
                        Text(
                            stringResource(R.string.end_desk_display),
                            color = WarmBlack,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeskControls(
    visible: Boolean,
    inDemo: Boolean,
    snap: com.musiccanvas.app.media.ConnectionSnapshot,
    viewModel: AppViewModel,
    playing: Boolean,
    onFinish: () -> Unit,
    remainingMs: Long,
) {
    if (!visible) return
    val canPrev = inDemo || snap.nowPlaying.canSkipPrevious
    val canPlay = inDemo || snap.nowPlaying.canPlayPause
    val canNext = inDemo || snap.nowPlaying.canSkipNext
    fun tint(on: Boolean) = if (on) PaperWhite else PaperWhite.copy(alpha = 0.3f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = PaperWhite.copy(alpha = 0.10f), modifier = Modifier.size(48.dp)) {
            IconButton(
                enabled = canPrev,
                onClick = {
                    if (inDemo) viewModel.demoPrevious()
                    else viewModel.sendCommand(PlayerCommand.PREVIOUS)
                },
            ) { Icon(Icons.Default.SkipPrevious, null, tint = tint(canPrev)) }
        }
        Spacer(Modifier.width(16.dp))
        Surface(
            shape = CircleShape,
            color = if (canPlay) Amber else Amber.copy(alpha = 0.3f),
            modifier = Modifier.size(56.dp),
        ) {
            IconButton(enabled = canPlay, onClick = {
                if (inDemo) viewModel.demoTogglePlay()
                else viewModel.sendCommand(PlayerCommand.PLAY_PAUSE)
            }) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, tint = WarmBlack,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Surface(shape = CircleShape, color = PaperWhite.copy(alpha = 0.10f), modifier = Modifier.size(48.dp)) {
            IconButton(
                enabled = canNext,
                onClick = {
                    if (inDemo) viewModel.demoNext()
                    else viewModel.sendCommand(PlayerCommand.NEXT)
                },
            ) { Icon(Icons.Default.SkipNext, null, tint = tint(canNext)) }
        }
        Spacer(Modifier.width(24.dp))
        Surface(shape = CircleShape, color = PaperWhite.copy(alpha = 0.10f), modifier = Modifier.size(48.dp)) {
            IconButton(onClick = onFinish) {
                Icon(Icons.Default.Close, null, tint = PaperWhite)
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            formatRemaining(remainingMs),
            color = Sage, style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun formatRemaining(ms: Long): String {
    val m = ms / 60_000
    val s = (ms % 60_000) / 1000
    return "%d:%02d".format(m, s)
}
