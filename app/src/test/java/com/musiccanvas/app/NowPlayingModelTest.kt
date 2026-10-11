package com.musiccanvas.app

import com.musiccanvas.app.media.NowPlaying
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** NowPlaying 字段降级规则（MC-020/024）：缺失不编造。 */
class NowPlayingModelTest {

    @Test
    fun `unknown duration stays null not zero`() {
        val np = NowPlaying(title = "t", artist = "a", durationMs = null)
        assertEquals(null, np.durationMs)
    }

    @Test
    fun `partial flag only when metadata exists but incomplete`() {
        val complete = NowPlaying(title = "t", artist = "a", durationMs = 1000)
        assertFalse(complete.isPartial)
        val partial = NowPlaying(title = "t", artist = null, durationMs = null, isPartial = true)
        assertTrue(partial.isPartial)
    }

    @Test
    fun `capabilities gate controls`() {
        val np = NowPlaying(canPlayPause = true, canSkipNext = false, canSkipPrevious = false)
        assertTrue(np.canPlayPause)
        assertFalse(np.canSkipNext)
        assertFalse(np.canSkipPrevious)
    }
}

class PositionEstimateTest {
    @Test
    fun `playing position advances by elapsed time times speed`() {
        val np = NowPlaying(positionMs = 30_000, positionUpdatedAtMs = 1_000, speed = 1f, isPlaying = true, durationMs = 200_000)
        assertEquals(37_000L, np.estimatedPositionMs(8_000))
    }

    @Test
    fun `paused position does not advance`() {
        val np = NowPlaying(positionMs = 30_000, positionUpdatedAtMs = 1_000, speed = 0f, isPlaying = false)
        assertEquals(30_000L, np.estimatedPositionMs(99_000))
    }

    @Test
    fun `estimate is clamped to duration and unknown stays null`() {
        val np = NowPlaying(positionMs = 190_000, positionUpdatedAtMs = 1_000, speed = 1f, isPlaying = true, durationMs = 200_000)
        assertEquals(200_000L, np.estimatedPositionMs(60_000))
        assertEquals(null, NowPlaying().estimatedPositionMs(60_000))
    }
}
