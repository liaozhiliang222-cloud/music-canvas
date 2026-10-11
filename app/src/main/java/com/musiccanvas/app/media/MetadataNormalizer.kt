package com.musiccanvas.app.media

import android.media.MediaMetadata
import android.media.session.PlaybackState

/** 来源差异在这里收敛（PRD 15.2）：空值、能力、封面全部标准化。 */
object MetadataNormalizer {

    private const val MAX_TEXT_LENGTH = 512

    fun normalize(
        metadata: MediaMetadata?,
        playbackState: PlaybackState?,
        trackGeneration: Long,
        previous: NowPlaying? = null,
    ): NowPlaying {
        val title = metadata
            ?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?.take(MAX_TEXT_LENGTH)
        val artist = metadata
            ?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?.take(MAX_TEXT_LENGTH)
        val duration = metadata
            ?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0 }

        val actions = playbackState?.actions ?: 0L
        val state = playbackState?.state ?: PlaybackState.STATE_NONE
        val isPlaying = state == PlaybackState.STATE_PLAYING ||
            state == PlaybackState.STATE_BUFFERING

        val position = playbackState
            ?.takeIf { duration != null }
            ?.position
            ?.takeIf { it >= 0 }

        // 时长未知且完全无元数据 → 仍显示“曲目信息暂不可用”，由 UI 判定 PARTIAL。
        val isPartial = metadata != null &&
            (title == null || artist == null || duration == null)

        return NowPlaying(
            title = title,
            artist = artist,
            durationMs = duration,
            positionMs = position,
            positionUpdatedAtMs = playbackState?.lastPositionUpdateTime ?: 0L,
            speed = playbackState?.playbackSpeed ?: 0f,
            isPlaying = isPlaying,
            canPlayPause = actions and (
                PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE
                ) != 0L,
            canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
            canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L,
            isPartial = isPartial,
            trackGeneration = trackGeneration,
        )
    }
}
