package com.musiccanvas.app.media

import android.graphics.Bitmap

/**
 * PRD 4.3 主要应用状态。
 * DEMO 与真实状态分离：Demo 轨道是独立数据类型，不与真实字段混用。
 */
enum class AppUiState {
    DEMO,
    PERMISSION_REQUIRED,
    WAITING_SOURCE,
    CONNECTED_PLAYING,
    CONNECTED_PAUSED,
    PARTIAL,
    DISCONNECTED,
}

enum class SourceMode { FIXED, AUTO_FOLLOW }

enum class ArtworkStatus { NONE, LOADING, READY, FAILED }

/** 用户允许的来源候选（本地持久化的是 packageId，不持久化会话 token）。 */
data class SourcePreference(
    val packageId: String,
    val allowed: Boolean,
)

/** 可被发现的媒体会话来源。 */
data class DiscoveredSource(
    val packageId: String,
    val appLabel: String,
    val sessionCount: Int,
)

/** 仅内存绑定的会话，不持久化；进程重启重新获取。 */
data class SessionBinding(
    val packageId: String,
    val sessionGeneration: Long,
)

/** 标准化后的当前播放模型：字段可空，缺失即降级，不编造。 */
data class NowPlaying(
    val title: String? = null,
    val artist: String? = null,
    val durationMs: Long? = null,
    val positionMs: Long? = null,
    val positionUpdatedAtMs: Long = 0L,
    val speed: Float = 0f,
    val isPlaying: Boolean = false,
    val canPlayPause: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val isPartial: Boolean = false,
    val trackGeneration: Long = 0L,
) {
    /** 按 PlaybackState 语义外推当前进度（时间基准为 elapsedRealtime）；未知则 null。 */
    fun estimatedPositionMs(nowElapsedMs: Long): Long? {
        val base = positionMs ?: return null
        if (!isPlaying || positionUpdatedAtMs <= 0L || speed <= 0f) return base
        val advanced = base + ((nowElapsedMs - positionUpdatedAtMs).coerceAtLeast(0L) * speed).toLong()
        return durationMs?.let { advanced.coerceAtMost(it) } ?: advanced
    }
}

data class ArtworkState(
    val generation: Long = 0L,
    val status: ArtworkStatus = ArtworkStatus.NONE,
    val bitmap: Bitmap? = null,
    val accentColor: Int? = null,
)

data class ConnectionSnapshot(
    val uiState: AppUiState = AppUiState.PERMISSION_REQUIRED,
    val permissionGranted: Boolean = false,
    val binding: SessionBinding? = null,
    val sourceMode: SourceMode = SourceMode.FIXED,
    val selectedPackageId: String? = null,
    val discovered: List<DiscoveredSource> = emptyList(),
    val allowedPackages: Set<String> = emptySet(),
    val nowPlaying: NowPlaying = NowPlaying(),
    val artwork: ArtworkState = ArtworkState(),
)

/** 演示状态专用数据，与真实数据类型隔离（PRD 4.4）。 */
data class DemoTrack(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val positionMs: Long,
    val isPlaying: Boolean,
    val artworkSeed: Int,
)
