package com.musiccanvas.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musiccanvas.app.R
import com.musiccanvas.app.ui.theme.Amber
import com.musiccanvas.app.ui.theme.PaperWhite
import com.musiccanvas.app.ui.theme.Sage
import com.musiccanvas.app.ui.theme.WarmBlack

/**
 * 解锁页（PRD 5.1 F，R1）。
 * B1 无真实支付：只展示权益说明与价格方案占位，不出现“购买修复连接”话术（MC-060）。
 */
@Composable
fun UnlockScreen(onBack: () -> Unit) {
    Surface(color = WarmBlack, modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Text(stringResource(R.string.unlock_page), style = MaterialTheme.typography.titleLarge)
            }

            Column(Modifier.padding(24.dp)) {
                Text("免费内容（当前已包含）", color = PaperWhite, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "· 暖木黑胶完整主题\n· 真实音乐连接与基础控制\n· 桌面模式、节能与隐私功能",
                    color = Sage, style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(24.dp))
                Text("Pro（计划中）", color = Amber, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "· 留白封面第二主题\n· 主题配色组合与额外布局\n· 一次性买断，非订阅",
                    color = Sage, style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.coming_r1) + "。买断权益仅适用于已列出的主题内容，不承诺全部未来更新。",
                    color = Sage.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
