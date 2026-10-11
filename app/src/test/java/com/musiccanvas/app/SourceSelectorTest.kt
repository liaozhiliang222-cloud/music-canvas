package com.musiccanvas.app

import com.musiccanvas.app.media.SourceMode
import com.musiccanvas.app.media.SourceSelector
import com.musiccanvas.app.media.SourceSelector.SourceCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceSelectorTest {

    private val netease = "com.netease.cloudmusic"
    private val qq = "com.tencent.qqmusic"
    private val video = "com.example.video"

    @Test
    fun `disallowed sources are never selected`() {
        val result = SourceSelector.selectPackage(
            candidates = listOf(SourceCandidate(video, isPlaying = true)),
            allowedPackages = setOf(netease),
            mode = SourceMode.AUTO_FOLLOW,
            fixedPackageId = null,
            currentBindingPackage = null,
        )
        assertNull(result)
    }

    @Test
    fun `fixed mode does not switch to another playing source`() {
        val result = SourceSelector.selectPackage(
            candidates = listOf(
                SourceCandidate(netease, isPlaying = false, order = 0),
                SourceCandidate(qq, isPlaying = true, order = 1),
            ),
            allowedPackages = setOf(netease, qq),
            mode = SourceMode.FIXED,
            fixedPackageId = netease,
            currentBindingPackage = netease,
        )
        // MC-012：固定 A 时 B 开始播放，不切换
        assertEquals(netease, result)
    }

    @Test
    fun `fixed mode returns null when fixed source absent`() {
        val result = SourceSelector.selectPackage(
            candidates = listOf(SourceCandidate(qq, isPlaying = true)),
            allowedPackages = setOf(netease, qq),
            mode = SourceMode.FIXED,
            fixedPackageId = netease,
            currentBindingPackage = netease,
        )
        assertNull(result)
    }

    @Test
    fun `auto follow prefers playing candidate`() {
        val result = SourceSelector.selectPackage(
            candidates = listOf(
                SourceCandidate(netease, isPlaying = false, order = 0),
                SourceCandidate(qq, isPlaying = true, order = 1),
            ),
            allowedPackages = setOf(netease, qq),
            mode = SourceMode.AUTO_FOLLOW,
            fixedPackageId = null,
            currentBindingPackage = null,
        )
        assertEquals(qq, result)
    }

    @Test
    fun `auto follow keeps current binding on tie`() {
        val result = SourceSelector.selectPackage(
            candidates = listOf(
                SourceCandidate(netease, isPlaying = false, order = 0),
                SourceCandidate(qq, isPlaying = false, order = 1),
            ),
            allowedPackages = setOf(netease, qq),
            mode = SourceMode.AUTO_FOLLOW,
            fixedPackageId = null,
            currentBindingPackage = netease,
        )
        // MC-013：平局保留当前，不抖动
        assertEquals(netease, result)
    }

    @Test
    fun `multiple sessions same package prefer playing`() {
        val result = SourceSelector.selectPackage(
            candidates = listOf(
                SourceCandidate(netease, isPlaying = false, order = 0),
                SourceCandidate(netease, isPlaying = true, order = 1),
            ),
            allowedPackages = setOf(netease),
            mode = SourceMode.FIXED,
            fixedPackageId = netease,
            currentBindingPackage = null,
        )
        assertEquals(netease, result)
    }

    @Test
    fun `auto follow switches when current paused and another starts playing`() {
        val result = SourceSelector.selectPackage(
            candidates = listOf(
                SourceCandidate(netease, isPlaying = false, order = 0),
                SourceCandidate(qq, isPlaying = true, order = 1),
            ),
            allowedPackages = setOf(netease, qq),
            mode = SourceMode.AUTO_FOLLOW,
            fixedPackageId = null,
            currentBindingPackage = netease,
        )
        assertEquals(qq, result)
    }

    @Test
    fun `auto follow keeps current when both playing`() {
        val result = SourceSelector.selectPackage(
            candidates = listOf(
                SourceCandidate(netease, isPlaying = true, order = 0),
                SourceCandidate(qq, isPlaying = true, order = 1),
            ),
            allowedPackages = setOf(netease, qq),
            mode = SourceMode.AUTO_FOLLOW,
            fixedPackageId = null,
            currentBindingPackage = qq,
        )
        assertEquals(qq, result)
    }

    @Test
    fun `empty candidates returns null`() {
        val result = SourceSelector.selectPackage(
            candidates = emptyList(),
            allowedPackages = setOf(netease),
            mode = SourceMode.AUTO_FOLLOW,
            fixedPackageId = null,
            currentBindingPackage = null,
        )
        assertNull(result)
    }
}
