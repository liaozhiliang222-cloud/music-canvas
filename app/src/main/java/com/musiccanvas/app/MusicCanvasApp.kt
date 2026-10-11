package com.musiccanvas.app

import android.app.Application
import com.musiccanvas.app.media.MediaSessionGateway
import com.musiccanvas.app.settings.SettingsStore

class MusicCanvasApp : Application() {

    lateinit var gateway: MediaSessionGateway
        private set
    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        gateway = MediaSessionGateway(this, settingsStore)
    }
}
