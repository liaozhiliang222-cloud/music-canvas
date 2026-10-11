package com.musiccanvas.app.media

/**
 * 选源纯逻辑（MC-010..MC-015），不依赖 Android 框架，可单测：
 * - 只处理用户明确允许的来源；
 * - 固定模式不偷换来源；
 * - 自动跟随只在已允许来源内生效，优先正在播放；平局保留当前。
 */
object SourceSelector {

    /** 轻量候选，由 Gateway 从 MediaController 适配而来。 */
    data class SourceCandidate(
        val packageId: String,
        val isPlaying: Boolean,
        val order: Int = 0,
    )

    fun selectPackage(
        candidates: List<SourceCandidate>,
        allowedPackages: Set<String>,
        mode: SourceMode,
        fixedPackageId: String?,
        currentBindingPackage: String?,
    ): String? {
        val allowed = candidates.filter { it.packageId in allowedPackages }
        if (allowed.isEmpty()) return null

        return when (mode) {
            SourceMode.FIXED -> {
                val target = fixedPackageId ?: currentBindingPackage ?: return null
                pickPlaying(allowed.filter { it.packageId == target })
            }
            SourceMode.AUTO_FOLLOW -> {
                val playing = allowed.filter { it.isPlaying }
                when {
                    // 当前来源仍在播放：保持，避免抖动
                    playing.any { it.packageId == currentBindingPackage } -> currentBindingPackage
                    // 另有来源在播放：跟随
                    playing.isNotEmpty() -> playing.minBy { it.order }.packageId
                    // 都没在播放：保留当前，否则取第一个
                    allowed.any { it.packageId == currentBindingPackage } -> currentBindingPackage
                    else -> allowed.minBy { it.order }.packageId
                }
            }
        }
    }

    private fun pickPlaying(candidates: List<SourceCandidate>): String? {
        return candidates.firstOrNull { it.isPlaying }?.packageId
            ?: candidates.minByOrNull { it.order }?.packageId
    }
}
