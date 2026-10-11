package com.musiccanvas.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musiccanvas.app.MusicCanvasApp
import com.musiccanvas.app.billing.EntitlementStore
import com.musiccanvas.app.billing.FreeBetaEntitlementStore
import com.musiccanvas.app.media.AppUiState
import com.musiccanvas.app.media.ConnectionSnapshot
import com.musiccanvas.app.media.DemoProvider
import com.musiccanvas.app.media.MediaSessionGateway
import com.musiccanvas.app.media.PlayerCommand
import com.musiccanvas.app.media.SourceMode
import com.musiccanvas.app.settings.MotionLevel
import com.musiccanvas.app.settings.PersistedPrefs
import com.musiccanvas.app.settings.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiModel(
    val snapshot: ConnectionSnapshot = ConnectionSnapshot(),
    val prefs: PersistedPrefs = PersistedPrefs(),
    val inDemoMode: Boolean = true,
    val demoPlaying: Boolean = true,
    val demoTrackIndex: Int = 0,
    val unlockedThemes: Set<String> = setOf("warm_vinyl"),
) {
    val demoTrack get() = DemoProvider.tracks[demoTrackIndex % DemoProvider.tracks.size]
}

class AppViewModel(context: Context) : ViewModel() {

    private val app = context.applicationContext as MusicCanvasApp
    private val gateway: MediaSessionGateway = app.gateway
    private val settings: SettingsStore = app.settingsStore
    private val entitlements: EntitlementStore = FreeBetaEntitlementStore()

    // 已授权用户直接进入真实模式；未授权进 Demo（PRD 4.1/4.2）。
    // 冷启动时 onResume 可能先于 Compose 赋值执行，不能只依赖 onForeground 翻转。
    private val inDemoMode = MutableStateFlow(!gateway.hasNotificationAccess())
    private val demoPlaying = MutableStateFlow(true)
    private val demoTrackIndex = MutableStateFlow(0)

    val ui: StateFlow<UiModel> = combine(
        gateway.snapshot,
        settings.prefs,
        inDemoMode,
        combine(demoPlaying, demoTrackIndex, entitlements.unlockedThemes) { p, i, u ->
            Triple(p, i, u)
        },
    ) { snap, prefs, demo, extra ->
        UiModel(
            snapshot = snap,
            prefs = prefs,
            inDemoMode = demo,
            demoPlaying = extra.first,
            demoTrackIndex = extra.second,
            unlockedThemes = extra.third,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiModel())

    init {
        gateway.startListening()
        // 授权后自动进入真实模式；授权被撤回时回到 Demo（不显示真实数据）。
        viewModelScope.launch {
            gateway.snapshot.collect { snap ->
                if (!snap.permissionGranted && !inDemoMode.value) {
                    enterDemo()
                }
            }
        }
    }

    /** 进入 Demo 预览：清除真实字段，避免误以为连接成功（PRD 4.4）。 */
    fun enterDemo() {
        inDemoMode.value = true
    }

    fun requestConnect(context: Context) {
        context.startActivity(gateway.openNotificationAccessSettings())
    }

    fun onForeground() {
        gateway.refreshSessions()
        if (gateway.hasNotificationAccess()) {
            inDemoMode.value = false
        }
    }

    fun onBackground() {
        // 后台停止插值；绘制层由 Composable 生命周期处理。
    }

    fun allowSource(packageId: String, allowed: Boolean) {
        val current = ui.value.snapshot.allowedPackages.toMutableSet()
        if (allowed) current.add(packageId) else current.remove(packageId)
        gateway.allowSources(current)
    }

    fun selectSource(mode: SourceMode, packageId: String?) {
        gateway.setSourceSelection(mode, packageId)
        if (packageId != null) {
            val current = ui.value.snapshot.allowedPackages.toMutableSet()
            if (current.add(packageId)) gateway.allowSources(current)
        }
    }

    fun disconnect() {
        gateway.disconnect()
        inDemoMode.value = true
    }

    fun demoTogglePlay() {
        demoPlaying.value = !demoPlaying.value
    }

    fun demoNext() {
        demoTrackIndex.value = (demoTrackIndex.value + 1) % DemoProvider.tracks.size
        demoPlaying.value = true
    }

    fun demoPrevious() {
        val size = DemoProvider.tracks.size
        demoTrackIndex.value = (demoTrackIndex.value - 1 + size) % size
        demoPlaying.value = true
    }

    fun sendCommand(command: PlayerCommand) {
        val snap = ui.value.snapshot
        if (snap.binding == null) return
        app.gateway.controlDispatcher.dispatch(command, snap.nowPlaying)
    }

    fun setMotion(level: MotionLevel) {
        viewModelScope.launch { settings.setMotionLevel(level) }
    }

    fun setHideInfo(hide: Boolean) {
        viewModelScope.launch { settings.setHideTrackInfo(hide) }
    }

    fun setTheme(themeId: String, variant: Int = 0) {
        viewModelScope.launch { settings.setTheme(themeId, variant) }
    }

    fun setDeskTimer(minutes: Int) {
        viewModelScope.launch { settings.setDeskTimer(minutes) }
    }

    fun setDeskHintShown() {
        viewModelScope.launch { settings.setDeskHintShown() }
    }

    fun clearCache() {
        viewModelScope.launch { settings.clearAll() }
    }

    fun appLabel(packageId: String): String = gateway.getAppLabel(packageId)

    fun openSourceApp(packageId: String, context: Context) {
        gateway.appLaunchIntent(packageId)?.let { context.startActivity(it) }
    }
}
