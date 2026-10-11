package com.musiccanvas.app.media

/** 免授权原创演示数据（MC-001）：不访问真实媒体，明确标注“演示”。 */
object DemoProvider {

    val tracks = listOf(
        DemoTrack("晨间木纹", "示例专辑 · 演示曲目", 198_000L, 42_000L, true, 0),
        DemoTrack("纸面航行", "示例专辑 · 演示曲目", 224_000L, 0L, false, 1),
        DemoTrack("琥珀色房间", "示例专辑 · 演示曲目", 181_000L, 96_000L, true, 2),
    )

    fun toNowPlaying(track: DemoTrack): NowPlaying = NowPlaying(
        title = track.title,
        artist = track.artist,
        durationMs = track.durationMs,
        positionMs = track.positionMs,
        positionUpdatedAtMs = System.currentTimeMillis(),
        speed = if (track.isPlaying) 1f else 0f,
        isPlaying = track.isPlaying,
        canPlayPause = true,
        canSkipNext = true,
        canSkipPrevious = true,
        isPartial = false,
        trackGeneration = -track.artworkSeed.toLong() - 1,
    )
}
