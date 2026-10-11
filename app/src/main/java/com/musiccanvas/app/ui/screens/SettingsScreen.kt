package com.musiccanvas.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musiccanvas.app.BuildConfig
import com.musiccanvas.app.R
import com.musiccanvas.app.ui.AppViewModel
import com.musiccanvas.app.ui.theme.PaperWhite
import com.musiccanvas.app.ui.theme.Sage
import com.musiccanvas.app.ui.theme.WarmBlack

/**
 * 设置与隐私（PRD 5.1 E）：断开连接、清理、诊断、版本。
 * 不自动更改系统设置（MC-005/MC-054）。
 */
@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    var confirmDisconnect by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Surface(color = WarmBlack, modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
            }

            SettingItem("连接状态") {
                Text(
                    text = if (ui.snapshot.permissionGranted) "已授权（仅所选来源）" else "未授权",
                    color = Sage, style = MaterialTheme.typography.bodyMedium,
                )
            }
            SettingItem(stringResource(R.string.desk_timer)) {
                Row {
                    listOf(15, 30, 60).forEach { m ->
                        TextButton(onClick = { viewModel.setDeskTimer(m) }) {
                            Text(
                                "$m 分钟",
                                color = if (ui.prefs.deskTimerMinutes == m)
                                    MaterialTheme.colorScheme.primary else Sage,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Sage.copy(alpha = 0.15f))

            SettingItem(stringResource(R.string.disconnect_connection)) {
                TextButton(onClick = { confirmDisconnect = true }) {
                    Text("断开并清除允许来源", color = MaterialTheme.colorScheme.error)
                }
            }
            SettingItem(stringResource(R.string.clear_cache)) {
                TextButton(onClick = { confirmClear = true }) {
                    Text("清除主题偏好与本地统计", color = Sage)
                }
            }
            SettingItem(stringResource(R.string.export_diagnostics)) {
                TextButton(onClick = { /* R1 */ }) {
                    Text("生成脱敏诊断（后续版本）", color = Sage)
                }
            }
            HorizontalDivider(color = Sage.copy(alpha = 0.15f))

            SettingItem(stringResource(R.string.version_label)) {
                Text(
                    "${BuildConfig.VERSION_NAME} · 本地优先，无账户",
                    color = Sage, style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                stringResource(R.string.privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = Sage,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (confirmDisconnect) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text(stringResource(R.string.disconnect_connection)) },
            text = {
                Text("将清除本地允许的音乐来源并停止控制。系统通知访问权限请在系统设置中关闭。")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDisconnect = false
                    viewModel.disconnect()
                }) { Text("确认断开") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDisconnect = false }) { Text("取消") }
            },
        )
    }
    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_cache)) },
            text = { Text("将清除主题偏好、允许来源与本地统计。不影响商店权益。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearCache()
                }) { Text("确认清除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SettingItem(title: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), color = PaperWhite)
        content()
    }
}
