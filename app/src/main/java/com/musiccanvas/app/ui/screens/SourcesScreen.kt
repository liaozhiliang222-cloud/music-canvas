package com.musiccanvas.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musiccanvas.app.R
import com.musiccanvas.app.media.SourceMode
import com.musiccanvas.app.ui.AppViewModel
import com.musiccanvas.app.ui.theme.PaperWhite
import com.musiccanvas.app.ui.theme.Sage
import com.musiccanvas.app.ui.theme.WarmBlack

/**
 * 来源页（PRD 5.1 C）：当前活动来源、用户允许的来源、能力说明。
 * 自动推荐不等于自动授权全部来源（MC-011）。
 */
@Composable
fun SourcesScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    val snap = ui.snapshot

    Surface(color = WarmBlack, modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Text(stringResource(R.string.sources), style = MaterialTheme.typography.titleLarge)
            }

            // 固定/自动跟随
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.auto_follow), color = PaperWhite)
                    Text(
                        "仅在你允许的来源内跟随正在播放的会话",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                    )
                }
                Switch(
                    checked = snap.sourceMode == SourceMode.AUTO_FOLLOW,
                    onCheckedChange = { auto ->
                        viewModel.selectSource(
                            if (auto) SourceMode.AUTO_FOLLOW else SourceMode.FIXED,
                            snap.selectedPackageId,
                        )
                    },
                )
            }
            HorizontalDivider(color = Sage.copy(alpha = 0.3f))

            Text(
                "发现的会话",
                style = MaterialTheme.typography.labelLarge,
                color = Sage,
                modifier = Modifier.padding(16.dp),
            )
            if (snap.discovered.isEmpty()) {
                Text(
                    stringResource(R.string.no_sources_found),
                    color = Sage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            LazyColumn {
                items(snap.discovered, key = { it.packageId }) { src ->
                    val allowed = src.packageId in snap.allowedPackages
                    val selected = snap.sourceMode == SourceMode.FIXED &&
                        snap.selectedPackageId == src.packageId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.allowSource(src.packageId, !allowed)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(src.appLabel, color = PaperWhite)
                            Text(
                                src.packageId + " · " +
                                    if (src.sessionCount > 1) "${src.sessionCount} 个会话（需确认）"
                                    else "1 个会话",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage,
                            )
                        }
                        Checkbox(
                            checked = allowed,
                            onCheckedChange = { viewModel.allowSource(src.packageId, it) },
                        )
                        if (snap.sourceMode == SourceMode.FIXED && allowed) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    viewModel.selectSource(SourceMode.FIXED, src.packageId)
                                },
                            )
                        }
                    }
                    HorizontalDivider(color = Sage.copy(alpha = 0.15f))
                }
            }

            Text(
                stringResource(R.string.privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = Sage,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
