package com.musiccanvas.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musiccanvas.app.media.SourceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "music_canvas")

enum class MotionLevel { STANDARD, SAVER, STATIC }

data class PersistedPrefs(
    val themeId: String = "warm_vinyl",
    val themeVariant: Int = 0,
    val motionLevel: MotionLevel = MotionLevel.STANDARD,
    val hideTrackInfo: Boolean = false,
    val deskTimerMinutes: Int = 15,
    val sourceMode: SourceMode = SourceMode.FIXED,
    val fixedPackageId: String? = null,
    val allowedSources: Set<String> = emptySet(),
    val deskHintShown: Boolean = false,
)

class SettingsStore(context: Context) {

    private val store = context.dataStore

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val THEME_VARIANT = intPreferencesKey("theme_variant")
        val MOTION = stringPreferencesKey("motion_level")
        val HIDE_INFO = booleanPreferencesKey("hide_track_info")
        val DESK_TIMER = intPreferencesKey("desk_timer_minutes")
        val SOURCE_MODE = stringPreferencesKey("source_mode")
        val FIXED_SOURCE = stringPreferencesKey("fixed_package_id")
        val ALLOWED = stringSetPreferencesKey("allowed_sources")
        val DESK_HINT = booleanPreferencesKey("desk_hint_shown")
    }

    val prefs: Flow<PersistedPrefs> = store.data.map { p ->
        PersistedPrefs(
            themeId = p[Keys.THEME_ID] ?: "warm_vinyl",
            themeVariant = p[Keys.THEME_VARIANT] ?: 0,
            motionLevel = p[Keys.MOTION]?.let { runCatching { MotionLevel.valueOf(it) }.getOrNull() }
                ?: MotionLevel.STANDARD,
            hideTrackInfo = p[Keys.HIDE_INFO] ?: false,
            deskTimerMinutes = p[Keys.DESK_TIMER] ?: 15,
            sourceMode = p[Keys.SOURCE_MODE]?.let { runCatching { SourceMode.valueOf(it) }.getOrNull() }
                ?: SourceMode.FIXED,
            fixedPackageId = p[Keys.FIXED_SOURCE],
            allowedSources = p[Keys.ALLOWED] ?: emptySet(),
            deskHintShown = p[Keys.DESK_HINT] ?: false,
        )
    }

    suspend fun setTheme(themeId: String, variant: Int) {
        store.edit { it[Keys.THEME_ID] = themeId; it[Keys.THEME_VARIANT] = variant }
    }

    suspend fun setMotionLevel(level: MotionLevel) {
        store.edit { it[Keys.MOTION] = level.name }
    }

    suspend fun setHideTrackInfo(hide: Boolean) {
        store.edit { it[Keys.HIDE_INFO] = hide }
    }

    suspend fun setDeskTimer(minutes: Int) {
        store.edit { it[Keys.DESK_TIMER] = minutes }
    }

    suspend fun setDeskHintShown() {
        store.edit { it[Keys.DESK_HINT] = true }
    }

    suspend fun setSourceMode(mode: SourceMode, fixedPackageId: String?) {
        store.edit {
            it[Keys.SOURCE_MODE] = mode.name
            if (fixedPackageId != null) it[Keys.FIXED_SOURCE] = fixedPackageId
        }
    }

    suspend fun setAllowedSources(packages: Set<String>) {
        store.edit { it[Keys.ALLOWED] = packages }
    }

    /** 清除缓存与偏好（MC-053）；不触碰权益数据（权益与媒体隔离）。 */
    suspend fun clearAll() {
        store.edit { it.clear() }
    }
}
