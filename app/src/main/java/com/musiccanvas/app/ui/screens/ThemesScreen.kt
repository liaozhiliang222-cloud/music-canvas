package com.musiccanvas.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.musiccanvas.app.settings.MotionLevel
import com.musiccanvas.app.ui.AppViewModel
import com.musiccanvas.app.ui.theme.Amber
import com.musiccanvas.app.ui.theme.PaperWhite
import com.musiccanvas.app.ui.theme.Sage
import com.musiccanvas.app.ui.theme.THEMES
import com.musiccanvas.app.ui.theme.WarmBlack

/**
 * 主题抽屉（PRD 5.1 B）：免费/预览、动效开关、信息显示。
 * Pro 主题可预览布局，但标注付费属性（不假装已解锁）。
 */
@Composable
fun ThemesScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenUnlock: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()

    Surface(color = WarmBlack, modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Text(stringResource(R.string.theme_drawer), style = MaterialTheme.typography.titleLarge)
            }

            LazyColumn {
                items(THEMES, key = { it.id }) { theme ->
                    val selected = ui.prefs.themeId == theme.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (theme.id in ui.unlockedThemes) {
                                    viewModel.setTheme(theme.id)
                                } else {
                                    onOpenUnlock()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(theme.displayName, color = PaperWhite)
                            Text(
                                when {
                                    !theme.isPro -> "免费主题"
                                    theme.id in ui.unlockedThemes -> stringResource(R.string.pro_theme_beta_free)
                                    else -> stringResource(R.string.pro_theme_locked)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (theme.isPro) Amber else Sage,
                            )
                        }
                        if (theme.isPro && theme.id !in ui.unlockedThemes) {
                            IconButton(onClick = onOpenUnlock) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Amber)
                            }
                        }
                        if (selected) {
                            Text("使用中", color = Amber, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    HorizontalDivider(color = Sage.copy(alpha = 0.15f))
                }

                item {
                    Text(
                        "动效档位",
                        style = MaterialTheme.typography.labelLarge,
                        color = Sage,
                        modifier = Modifier.padding(16.dp),
                    )
                    listOf(
                        MotionLevel.STANDARD to R.string.motion_standard,
                        MotionLevel.SAVER to R.string.motion_saver,
                        MotionLevel.STATIC to R.string.motion_static,
                    ).forEach { (level, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setMotion(level) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(label), Modifier.weight(1f), color = PaperWhite)
                            if (ui.prefs.motionLevel == level) {
                                Text("●", color = Amber)
                            }
                        }
                    }
                    HorizontalDivider(color = Sage.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.hide_track_info),
                            Modifier.weight(1f),
                            color = PaperWhite,
                        )
                        Switch(
                            checked = ui.prefs.hideTrackInfo,
                            onCheckedChange = { viewModel.setHideInfo(it) },
                        )
                    }
                }
            }
        }
    }
}
