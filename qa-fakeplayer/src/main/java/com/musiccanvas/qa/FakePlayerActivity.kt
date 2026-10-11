package com.musiccanvas.qa

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.SystemClock
import android.widget.TextView

/**
 * QA 假播放器：发布一个标准 MediaSession。
 * adb 驱动：am broadcast -n <pkg>/com.musiccanvas.qa.FakePlayerReceiver --es cmd play|pause|next|partial|noart
 */
object FakeSession {
    private var session: MediaSession? = null
    private var index = 0
    private var playing = false
    private var partial = false
    private var withArt = true
    val log = mutableListOf<String>()
    private val tracks = listOf(
        Triple("测试曲一", "测试艺人甲", 200_000L),
        Triple("测试曲二", "测试艺人乙", 185_000L),
        Triple("测试曲三", "测试艺人丙", 240_000L),
    )

    fun ensure(ctx: Context, tag: String): MediaSession {
        session?.let { return it }
        val s = MediaSession(ctx.applicationContext, tag)
        s.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { log += "onPlay"; playing = true; publish(ctx) }
            override fun onPause() { log += "onPause"; playing = false; publish(ctx) }
            override fun onSkipToNext() { log += "onNext"; index++; publish(ctx) }
            override fun onSkipToPrevious() { log += "onPrev"; index--; publish(ctx) }
        })
        s.isActive = true
        session = s
        return s
    }

    fun handle(ctx: Context, cmd: String) {
        ensure(ctx, ctx.packageName)
        when (cmd) {
            "play" -> playing = true
            "pause" -> playing = false
            "next" -> index++
            "partial" -> partial = true
            "full" -> partial = false
            "noart" -> withArt = false
            "art" -> withArt = true
        }
        publish(ctx)
    }

    fun publish(ctx: Context) {
        val s = ensure(ctx, ctx.packageName)
        val t = tracks[Math.floorMod(index, tracks.size)]
        val b = MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, t.first)
        if (!partial) {
            b.putString(MediaMetadata.METADATA_KEY_ARTIST, t.second)
            b.putLong(MediaMetadata.METADATA_KEY_DURATION, t.third)
        }
        if (withArt) b.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art(index, ctx.packageName))
        s.setMetadata(b.build())
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
            (if (partial) 0L else PlaybackState.ACTION_SKIP_TO_PREVIOUS)
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(
                    if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    30_000L, if (playing) 1f else 0f, SystemClock.elapsedRealtime(),
                ).build(),
        )
    }

    private fun art(i: Int, pkg: String): Bitmap {
        val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val base = if (pkg.endsWith("beta")) Color.rgb(60, 110, 160) else Color.rgb(170, 70, 60)
        c.drawColor(base)
        val p = Paint().apply { color = Color.WHITE; textSize = 120f; isAntiAlias = true }
        c.drawText("${Math.floorMod(i, 3) + 1}", 95f, 170f, p)
        return bmp
    }
}

class FakePlayerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FakeSession.handle(context, intent.getStringExtra("cmd") ?: "play")
    }
}

class FakePlayerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FakeSession.handle(this, "play")
        setContentView(TextView(this).apply { text = "${packageName}\nMediaSession active"; textSize = 20f })
    }
}
