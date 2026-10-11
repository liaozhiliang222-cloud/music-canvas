package com.musiccanvas.app.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 系统绑定本服务后，MediaSessionManager.getActiveSessions 才能以本组件名义
 * 返回活动媒体会话。本服务不解析、不保存、不上传任何通知正文（PRD 17.2）。
 */
class MediaListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        MediaSessionGatewayHolder.onListenerConnected(applicationContext)
    }

    override fun onListenerDisconnected() {
        MediaSessionGatewayHolder.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // 故意不处理通知内容：只用媒体会话通道。
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // 同上。
    }
}

/** 让服务回调能通知到 Application 持有的 Gateway（避免服务直接持有 UI）。 */
object MediaSessionGatewayHolder {
    private var onConnected: ((android.content.Context) -> Unit)? = null
    private var onDisconnected: (() -> Unit)? = null

    fun register(
        connected: (android.content.Context) -> Unit,
        disconnected: () -> Unit,
    ) {
        onConnected = connected
        onDisconnected = disconnected
    }

    fun onListenerConnected(context: android.content.Context) {
        onConnected?.invoke(context)
    }

    fun onListenerDisconnected() {
        onDisconnected?.invoke()
    }
}
