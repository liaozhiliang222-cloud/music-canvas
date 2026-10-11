package com.musiccanvas.app.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.graphics.drawable.toBitmap
import com.musiccanvas.app.settings.SettingsStore
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 权限状态、系统绑定、回调订阅的唯一入口（PRD 15.2）。
 * 不直接更新主题界面：只产出 ConnectionSnapshot，由 ViewModel 消费。
 */
class MediaSessionGateway(
    private val context: Context,
    private val settingsStore: SettingsStore,
) {

    // 所有会话状态在主线程串行变更；仅封面解码切到 IO。
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent =
        ComponentName(context, MediaListenerService::class.java)

    private val _snapshot = MutableStateFlow(ConnectionSnapshot())
    val snapshot: StateFlow<ConnectionSnapshot> = _snapshot

    val controlDispatcher = ControlDispatcher(scope)

    private val sessionGeneration = AtomicLong(0L)
    private val trackGeneration = AtomicLong(0L)

    private var activeControllers: List<MediaController> = emptyList()
    private var boundController: MediaController? = null
    private var artworkLoadGeneration = 0L

    /** 通知监听服务被系统绑定/解绑时回调。 */
    private var listenerConnected = false

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            scope.launch { onSessionsChanged(controllers.orEmpty()) }
        }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            scope.launch { onMetadata(metadata) }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            scope.launch { onPlaybackState(state) }
        }

        override fun onSessionDestroyed() {
            scope.launch { refreshSelection() }
        }
    }

    /** 自动跟随时观察其他已允许来源的播放状态；只用于触发重选，不读元数据。 */
    private val watcherCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            scope.launch { refreshSelection() }
        }
    }
    private var watchedControllers: List<MediaController> = emptyList()

    private fun updateWatchers(mode: SourceMode, allowed: Set<String>) {
        val next = if (mode == SourceMode.AUTO_FOLLOW) {
            activeControllers.filter { it.packageName in allowed }
        } else {
            emptyList()
        }
        if (next.size == watchedControllers.size &&
            next.zip(watchedControllers).all { (a, b) -> a.sessionToken == b.sessionToken }
        ) return
        watchedControllers.forEach { runCatching { it.unregisterCallback(watcherCallback) } }
        watchedControllers = next
        watchedControllers.forEach { runCatching { it.registerCallback(watcherCallback, mainHandler) } }
    }

    init {
        MediaSessionGatewayHolder.register(
            connected = { scope.launch { onListenerConnected() } },
            disconnected = { scope.launch { onListenerDisconnected() } },
        )
        // 用户在系统设置中撤销/授予通知访问时立即响应（MC-005）；不依赖服务断开回调。
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("enabled_notification_listeners"),
            false,
            object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) {
                    refreshSessions()
                }
            },
        )
        controlDispatcher.bindingProvider = { _snapshot.value.binding }
        controlDispatcher.controllerProvider = { boundController }
        scope.launch { refreshPermissionState() }
    }

    fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return enabled.split(':').any {
            ComponentName.unflattenFromString(it)?.packageName == context.packageName
        }
    }

    fun openNotificationAccessSettings(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun startListening() {
        refreshSessions()
    }

    fun stopListening() {
        unregisterSessionsListener()
        updateWatchers(SourceMode.FIXED, emptySet())
        unbind()
    }

    private var sessionsListenerRegistered = false

    /** 未授权时注册会抛 SecurityException；授权后每次刷新按需补注册。 */
    private fun ensureSessionsListener(): Boolean {
        if (sessionsListenerRegistered) return true
        sessionsListenerRegistered = runCatching {
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener, listenerComponent, mainHandler,
            )
        }.isSuccess
        return sessionsListenerRegistered
    }

    private fun unregisterSessionsListener() {
        if (!sessionsListenerRegistered) return
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener) }
        sessionsListenerRegistered = false
    }

    fun refreshSessions() {
        val granted = hasNotificationAccess()
        if (!granted) {
            unregisterSessionsListener()
            updateWatchers(SourceMode.FIXED, emptySet())
            activeControllers = emptyList()
            unbind()
            _snapshot.value = _snapshot.value.copy(
                uiState = AppUiState.PERMISSION_REQUIRED,
                permissionGranted = false,
                nowPlaying = NowPlaying(),
                artwork = ArtworkState(),
            )
            return
        }
        ensureSessionsListener()
        runCatching {
            activeControllers = sessionManager
                .getActiveSessions(listenerComponent)
                .orEmpty()
        }.onSuccess { scope.launch { refreshSelection() } }
            .onFailure {
                _snapshot.value = _snapshot.value.copy(uiState = AppUiState.DISCONNECTED)
            }
    }

    fun setSourceSelection(mode: SourceMode, fixedPackageId: String?) {
        scope.launch {
            settingsStore.setSourceMode(mode, fixedPackageId)
            _snapshot.value = _snapshot.value.copy(
                sourceMode = mode,
                selectedPackageId = fixedPackageId ?: _snapshot.value.selectedPackageId,
            )
            refreshSelection()
        }
    }

    fun allowSources(packages: Set<String>) {
        scope.launch {
            settingsStore.setAllowedSources(packages)
            _snapshot.value = _snapshot.value.copy(allowedPackages = packages)
            refreshSelection()
        }
    }

    fun disconnect() {
        // MC-054：撤销本地允许来源、停止控制；系统授权由用户自行关闭（提供指引）。
        unbind()
        scope.launch {
            settingsStore.setAllowedSources(emptySet())
            settingsStore.setSourceMode(SourceMode.FIXED, null)
        }
        _snapshot.value = _snapshot.value.copy(
            uiState = if (hasNotificationAccess()) AppUiState.WAITING_SOURCE
            else AppUiState.PERMISSION_REQUIRED,
            binding = null,
            allowedPackages = emptySet(),
            nowPlaying = NowPlaying(),
            artwork = ArtworkState(),
        )
    }

    // ---- internal ----

    private fun onListenerConnected() {
        listenerConnected = true
        refreshSessions()
    }

    private fun onListenerDisconnected() {
        listenerConnected = false
        unbind()
        _snapshot.value = _snapshot.value.copy(uiState = AppUiState.DISCONNECTED)
        // 可能是用户撤销了通知访问：立即重检，未授权则进入 PERMISSION_REQUIRED（MC-005）
        if (!hasNotificationAccess()) refreshSessions()
    }

    private suspend fun onSessionsChanged(controllers: List<MediaController>) {
        activeControllers = controllers
        refreshSelection()
    }

    private suspend fun refreshSelection() {
        val prefs = settingsStore.prefs.first()
        updateWatchers(prefs.sourceMode, prefs.allowedSources)
        val discovered = discoverSources(activeControllers)
        val candidates = activeControllers.mapIndexed { index, c ->
            SourceSelector.SourceCandidate(
                packageId = c.packageName,
                isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
                order = index,
            )
        }
        val selectedPackage = SourceSelector.selectPackage(
            candidates = candidates,
            allowedPackages = prefs.allowedSources,
            mode = prefs.sourceMode,
            fixedPackageId = prefs.fixedPackageId,
            currentBindingPackage = _snapshot.value.binding?.packageId,
        )

        _snapshot.value = _snapshot.value.copy(
            permissionGranted = hasNotificationAccess(),
            discovered = discovered,
            allowedPackages = prefs.allowedSources,
            sourceMode = prefs.sourceMode,
            selectedPackageId = prefs.fixedPackageId,
        )

        val controller = selectedPackage?.let { pkg ->
            // 多会话消歧：同包多个会话优先正在播放者（MC-014）
            activeControllers.filter { it.packageName == pkg }
                .firstOrNull {
                    it.playbackState?.state == PlaybackState.STATE_PLAYING
                }
                ?: activeControllers.firstOrNull { it.packageName == pkg }
        }
        if (controller == null) {
            unbind()
            val next = if (!hasNotificationAccess()) AppUiState.PERMISSION_REQUIRED
            else AppUiState.WAITING_SOURCE
            _snapshot.value = _snapshot.value.copy(uiState = next)
            return
        }
        bind(controller)
    }

    private fun bind(controller: MediaController) {
        // 固定来源不变更：相同包名且已有绑定则复用。
        val current = _snapshot.value.binding
        if (current != null && current.packageId == controller.packageName &&
            boundController === controller
        ) {
            return
        }
        unbind()
        val binding = SessionBinding(
            packageId = controller.packageName,
            sessionGeneration = sessionGeneration.incrementAndGet(),
        )
        boundController = controller
        runCatching { controller.registerCallback(controllerCallback, mainHandler) }
        _snapshot.value = _snapshot.value.copy(binding = binding)
        pushCurrentState(controller)
    }

    private fun unbind() {
        boundController?.let { runCatching { it.unregisterCallback(controllerCallback) } }
        boundController = null
        _snapshot.value = _snapshot.value.copy(
            binding = null,
            nowPlaying = NowPlaying(),
            artwork = ArtworkState(),
        )
    }

    private fun pushCurrentState(controller: MediaController) {
        val binding = _snapshot.value.binding ?: return
        val newTrackGen = trackGeneration.incrementAndGet()
        val normalized = MetadataNormalizer.normalize(
            controller.metadata, controller.playbackState, newTrackGen,
        )
        val ui = when {
            normalized.isPlaying -> AppUiState.CONNECTED_PLAYING
            normalized.isPartial -> AppUiState.PARTIAL
            else -> AppUiState.CONNECTED_PAUSED
        }
        _snapshot.value = _snapshot.value.copy(
            uiState = ui,
            nowPlaying = normalized,
            artwork = ArtworkState(generation = newTrackGen, status = ArtworkStatus.LOADING),
        )
        loadArtwork(controller, binding, newTrackGen)
    }

    private fun onMetadata(metadata: MediaMetadata?) {
        val controller = boundController ?: return
        pushCurrentState(controller)
    }

    private fun onPlaybackState(state: PlaybackState?) {
        controlDispatcher.onPlaybackChanged()
        val controller = boundController ?: return
        val binding = _snapshot.value.binding ?: return
        val current = _snapshot.value.nowPlaying
        val normalized = MetadataNormalizer.normalize(
            controller.metadata, state, current.trackGeneration,
        )
        val ui = when {
            normalized.isPlaying -> AppUiState.CONNECTED_PLAYING
            normalized.isPartial -> AppUiState.PARTIAL
            else -> AppUiState.CONNECTED_PAUSED
        }
        _snapshot.value = _snapshot.value.copy(
            uiState = ui,
            nowPlaying = normalized,
        )
    }

    /** 封面只使用来源提供的数据；异步结果校验 generation（MC-021/023/025）。 */
    private fun loadArtwork(
        controller: MediaController,
        binding: SessionBinding,
        generation: Long,
    ) {
        val metadata = controller.metadata ?: run {
            _snapshot.value = _snapshot.value.copy(
                artwork = ArtworkState(generation = generation, status = ArtworkStatus.FAILED),
            )
            return
        }
        val loadGen = ++artworkLoadGeneration

        val embedded = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (embedded != null) {
            applyArtwork(downscale(embedded), generation, loadGen)
            return
        }
        val uri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
        if (uri.isNullOrBlank() || uri.startsWith("http")) {
            // 不主动发起网络请求（PRD 10.3）。
            _snapshot.value = _snapshot.value.copy(
                artwork = ArtworkState(generation = generation, status = ArtworkStatus.FAILED),
            )
            return
        }
        scope.launch {
            val decoded = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { input ->
                        val bytes = input.readBytes()
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }?.let { downscale(it) }
                }.getOrNull()
            }
            if (decoded != null) {
                applyArtwork(decoded, generation, loadGen)
            } else if (loadGen == artworkLoadGeneration &&
                _snapshot.value.nowPlaying.trackGeneration == generation
            ) {
                _snapshot.value = _snapshot.value.copy(
                    artwork = ArtworkState(generation = generation, status = ArtworkStatus.FAILED),
                )
            }
        }
    }

    private fun applyArtwork(bitmap: Bitmap, trackGen: Long, loadGen: Long) {
        // 旧封面迟到且新歌已切换 → 丢弃，不串配（QA-06）。
        if (loadGen != artworkLoadGeneration) return
        if (_snapshot.value.nowPlaying.trackGeneration != trackGen) return
        _snapshot.value = _snapshot.value.copy(
            artwork = ArtworkState(
                generation = trackGen,
                status = ArtworkStatus.READY,
                bitmap = bitmap,
                accentColor = extractAccent(bitmap),
            ),
        )
    }

    /** 封面解码限制长边 1024px（MC-022）。 */
    private fun downscale(source: Bitmap): Bitmap {
        val max = 1024
        val w = source.width
        val h = source.height
        if (w <= max && h <= max) return source
        val scale = max.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(
            source, (w * scale).toInt(), (h * scale).toInt(), true
        )
    }

    private fun extractAccent(bitmap: Bitmap): Int? = runCatching {
        val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
        palette.getMutedColor(palette.getVibrantColor(0xFFC99454.toInt()))
    }.getOrNull()

    private fun discoverSources(controllers: List<MediaController>): List<DiscoveredSource> {
        val pm = context.packageManager
        return controllers
            .groupBy { it.packageName }
            .map { (pkg, list) ->
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                DiscoveredSource(packageId = pkg, appLabel = label, sessionCount = list.size)
            }
            .sortedBy { it.appLabel }
    }

    fun appLaunchIntent(packageId: String): Intent? =
        context.packageManager.getLaunchIntentForPackage(packageId)

    fun getAppLabel(packageId: String): String = runCatching {
        context.packageManager.getApplicationLabel(
            context.packageManager.getApplicationInfo(packageId, 0)
        ).toString()
    }.getOrDefault(packageId)

    private suspend fun refreshPermissionState() {
        _snapshot.value = _snapshot.value.copy(
            permissionGranted = hasNotificationAccess(),
            uiState = if (hasNotificationAccess()) AppUiState.WAITING_SOURCE
            else AppUiState.PERMISSION_REQUIRED,
        )
    }
}
