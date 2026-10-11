package com.musiccanvas.app.media

import android.media.session.MediaController
import android.media.session.PlaybackState
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

enum class CommandResult { SENT, REJECTED, TIMEOUT }

enum class PlayerCommand { PLAY_PAUSE, NEXT, PREVIOUS }

/**
 * 命令发送前必须校验：权限、绑定快照未变、动作在能力集合内。
 * 单次点击只发一次；连点节流；超时不自动重发（MC-030..035）。
 */
class ControlDispatcher(
    private val scope: CoroutineScope,
    private val commandTimeoutMs: Long = 2000L,
    private val throttleMs: Long = 350L,
) {

    private val lastClickAt = AtomicLong(0L)
    private var timeoutJob: Job? = null

    private val _events = MutableSharedFlow<CommandResult>(extraBufferCapacity = 8)
    val events: SharedFlow<CommandResult> = _events

    /** 由网关在当前控制器/能力变化时更新。 */
    @Volatile
    var controllerProvider: () -> MediaController? = { null }

    @Volatile
    var bindingProvider: () -> SessionBinding? = { null }

    fun dispatch(command: PlayerCommand, now: NowPlaying): CommandResult {
        val binding = bindingProvider() ?: return emit(CommandResult.REJECTED)
        val controller = controllerProvider() ?: return emit(CommandResult.REJECTED)

        // 能力校验（MC-031）：不支持的动作直接拒绝，不发送。
        val allowed = when (command) {
            PlayerCommand.PLAY_PAUSE -> now.canPlayPause
            PlayerCommand.NEXT -> now.canSkipNext
            PlayerCommand.PREVIOUS -> now.canSkipPrevious
        }
        if (!allowed) return emit(CommandResult.REJECTED)

        // 防串源：绑定快照必须仍是当前绑定（MC-034）。
        if (bindingProvider() != binding) return emit(CommandResult.REJECTED)

        // 节流（MC-032）。
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastClickAt.get() < throttleMs) return emit(CommandResult.REJECTED)
        lastClickAt.set(nowMs)

        val controls = controller.transportControls
        when (command) {
            PlayerCommand.PLAY_PAUSE -> if (now.isPlaying) controls.pause() else controls.play()
            PlayerCommand.NEXT -> controls.skipToNext()
            PlayerCommand.PREVIOUS -> controls.skipToPrevious()
        }

        // 超时提示：2s 未收到状态回调即提示“来源暂未响应”，不自动重发（MC-033）。
        timeoutJob?.cancel()
        val generation = binding.sessionGeneration
        timeoutJob = scope.launch {
            delay(commandTimeoutMs)
            val current = bindingProvider()
            if (current != null && current.sessionGeneration == generation) {
                _events.emit(CommandResult.TIMEOUT)
            }
        }

        return emit(CommandResult.SENT)
    }

    /** 来源状态回调到达即确认命令已被处理。 */
    fun onPlaybackChanged() {
        timeoutJob?.cancel()
    }

    private fun emit(result: CommandResult): CommandResult {
        _events.tryEmit(result)
        return result
    }
}
