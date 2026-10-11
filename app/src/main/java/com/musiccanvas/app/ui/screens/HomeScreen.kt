package com.musiccanvas.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musiccanvas.app.R
import com.musiccanvas.app.desk.DeskActivity
import com.musiccanvas.app.media.AppUiState
import com.musiccanvas.app.media.DemoProvider
import com.musiccanvas.app.media.PlayerCommand
import com.musiccanvas.app.media.SourceMode
import com.musiccanvas.app.ui.AppViewModel
import com.musiccanvas.app.ui.theme.ArtworkArea
import com.musiccanvas.app.ui.theme.Amber
import com.musiccanvas.app.ui.theme.PaperWhite
import com.musiccanvas.app.ui.theme.Sage
import com.musiccanvas.app.ui.theme.WarmBlack

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onOpenSources: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenUnlock: () -> Unit,
    context: Context,
) {
    val ui by viewModel.ui.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }

    val snap = ui.snapshot
    val inDemo = ui.inDemoMode || !snap.permissionGranted

    val playing: Boolean
    val title: String?
    val artist: String?
    val duration: Long?
    val position: Long?
    val artwork = if (inDemo) null else snap.artwork.bitmap
    val accent = snap.artwork.accentColor

    if (inDemo) {
        val track = ui.demoTrack
        playing = ui.demoPlaying
        title = track.title
        artist = track.artist
        duration = track.durationMs
        position = track.positionMs
    } else {
        val np = snap.nowPlaying
        playing = np.isPlaying
        title = np.title
        artist = np.artist
        duration = np.durationMs
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(np.isPlaying, np.positionUpdatedAtMs) {
            while (np.isPlaying) {
                now = SystemClock.elapsedRealtime()
                delay(1000)
            }
        }
        position = np.estimatedPositionMs(now)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(WarmBlack, WarmBlack.copy(alpha = 0.96f))
                )
            )
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部状态区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (inDemo) {
                    Surface(
                        color = Sage.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.demo_badge),
                            color = PaperWhite,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                } else {
                    SourceStatusChip(
                        uiState = snap.uiState,
                        sourceLabel = (snap.binding?.packageId ?: snap.selectedPackageId)
                            ?.let { pkg -> snap.discovered.firstOrNull { it.packageId == pkg }?.appLabel ?: viewModel.appLabel(pkg) }
                            ?: "",
                        autoFollow = snap.sourceMode == SourceMode.AUTO_FOLLOW,
                        viewModel = viewModel,
                        onClick = onOpenSources,
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                }
            }

            // 主题主体：竖屏占 55–65%
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.60f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                ArtworkArea(
                    themeId = ui.prefs.themeId,
                    artwork = artwork,
                    isPlaying = playing,
                    motionLevel = ui.prefs.motionLevel,
                    accentColor = accent,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            val statusMessage = if (inDemo) null else when (snap.uiState) {
                AppUiState.WAITING_SOURCE -> stringResource(R.string.waiting_source)
                AppUiState.DISCONNECTED -> stringResource(R.string.disconnected)
                else -> null
            }

            // 信息区
            if (statusMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(statusMessage, color = PaperWhite, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.waiting_source_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                    )
                    Spacer(Modifier.height(12.dp))
                    val pkg = snap.selectedPackageId
                    Button(onClick = {
                        if (pkg != null) viewModel.openSourceApp(pkg, context) else onOpenSources()
                    }) {
                        Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(
                                if (pkg != null) R.string.open_source_app else R.string.sources,
                            ),
                        )
                    }
                }
            } else if (!ui.prefs.hideTrackInfo) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title ?: stringResource(R.string.track_info_unavailable),
                        style = MaterialTheme.typography.titleLarge,
                        color = PaperWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = artist ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sage,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!inDemo && snap.uiState == AppUiState.PARTIAL) {
                        Text(
                            text = stringResource(R.string.partial_fields),
                            style = MaterialTheme.typography.labelSmall,
                            color = Sage.copy(alpha = 0.7f),
                        )
                    }
                    ProgressLine(
                        position = position,
                        duration = duration,
                        isPlaying = playing,
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }

            // 控制区（无已绑定来源时隐藏，MC-030）
            if (statusMessage == null) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(
                    enabled = inDemo || snap.nowPlaying.canSkipPrevious,
                    onClick = {
                        if (inDemo) viewModel.demoPrevious()
                        else viewModel.sendCommand(PlayerCommand.PREVIOUS)
                    },
                ) { Icon(Icons.Default.SkipPrevious, null, tint = PaperWhite) }
                Spacer(Modifier.width(20.dp))
                val canPlay = inDemo || snap.nowPlaying.canPlayPause
                Surface(
                    shape = CircleShape,
                    color = if (canPlay) Amber else Amber.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(64.dp)
                        .clickable(enabled = canPlay) {
                            if (inDemo) viewModel.demoTogglePlay()
                            else viewModel.sendCommand(PlayerCommand.PLAY_PAUSE)
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = WarmBlack,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
                ControlButton(
                    enabled = inDemo || snap.nowPlaying.canSkipNext,
                    onClick = {
                        if (inDemo) viewModel.demoNext()
                        else viewModel.sendCommand(PlayerCommand.NEXT)
                    },
                ) { Icon(Icons.Default.SkipNext, null, tint = PaperWhite) }
            }

            // 底部操作区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onOpenThemes) {
                    Text(stringResource(R.string.theme_drawer), color = PaperWhite)
                }
                if (inDemo) {
                    TextButton(onClick = { showPermissionDialog = true }) {
                        Text(stringResource(R.string.connect_my_music), color = Amber)
                    }
                } else {
                    TextButton(onClick = onOpenSources) {
                        Text(stringResource(R.string.sources), color = PaperWhite)
                    }
                }
                TextButton(
                    onClick = {
                        val intent = Intent(context, DeskActivity::class.java)
                        context.startActivity(intent)
                    },
                ) {
                    Text(stringResource(R.string.start_desk_display), color = PaperWhite)
                }
            }
        }

    }

    // 授权说明（MC-002）：展示用途与范围，拒绝/关闭后仍可预览
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.permission_title)) },
            text = { Text(stringResource(R.string.permission_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    viewModel.requestConnect(context)
                }) {
                    Text(stringResource(R.string.permission_go_settings), color = Amber)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.permission_later))
                }
            },
        )
    }
}

@Composable
private fun SourceStatusChip(
    uiState: AppUiState,
    sourceLabel: String,
    autoFollow: Boolean,
    viewModel: AppViewModel,
    onClick: () -> Unit,
) {
    val (text, dotColor) = when (uiState) {
        AppUiState.CONNECTED_PLAYING -> stringResource(
            if (autoFollow) R.string.auto_follow_short else R.string.fixed_source
        ) to androidx.compose.ui.graphics.Color(0xFF7FBF7F)
        AppUiState.CONNECTED_PAUSED -> "已暂停" to Sage
        AppUiState.PARTIAL -> stringResource(R.string.partial_fields) to Sage
        AppUiState.WAITING_SOURCE -> stringResource(R.string.waiting_source) to Sage.copy(0.6f)
        AppUiState.DISCONNECTED -> stringResource(R.string.disconnected) to MaterialTheme.colorScheme.error
        else -> "" to Sage
    }
    Surface(
        color = Sage.copy(alpha = 0.18f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (sourceLabel.isBlank()) text else "$sourceLabel · $text",
                style = MaterialTheme.typography.labelMedium,
                color = PaperWhite,
            )
        }
    }
}

@Composable
private fun ControlButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) PaperWhite.copy(alpha = 0.12f) else PaperWhite.copy(alpha = 0.04f),
        modifier = Modifier
            .size(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun ProgressLine(position: Long?, duration: Long?, isPlaying: Boolean) {
    if (position == null || duration == null || duration <= 0) {
        Spacer(Modifier.height(16.dp))
        return
    }
    val frac = (position.toFloat() / duration).coerceIn(0f, 1f)
    Spacer(Modifier.height(8.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(PaperWhite.copy(alpha = 0.15f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(frac)
                .height(2.dp)
                .background(Amber),
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(formatMs(position), style = MaterialTheme.typography.labelSmall, color = Sage)
        Text(formatMs(duration), style = MaterialTheme.typography.labelSmall, color = Sage)
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
