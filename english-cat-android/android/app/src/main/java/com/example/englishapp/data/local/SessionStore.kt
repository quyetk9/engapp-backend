package com.example.englishapp.data.local

import android.content.Context
import com.example.englishapp.BuildConfig
import org.json.JSONObject

data class OfflineProgress(
    val xp: Int = 0,
    val streak: Int = 0,
    val lastDay: String = "",
    val bestAccByLesson: Map<Int, Int> = emptyMap(),
)

/**
 * Lưu phiên đăng nhập, URL backend tùy chỉnh và tiến độ học offline trên thiết bị.
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("token", null)
        set(value) = prefs.edit().apply {
            if (value == null) remove("token") else putString("token", value)
        }.apply()

    var offlineMode: Boolean
        get() = prefs.getBoolean("offline_mode", false)
        set(value) = prefs.edit().putBoolean("offline_mode", value).apply()

    var backendUrl: String
        get() = prefs.getString("backend_url", null)?.takeIf { it.isNotBlank() } ?: BuildConfig.API_BASE_URL
        set(value) = prefs.edit().apply {
            val clean = value.trim()
            if (clean.isEmpty()) remove("backend_url") else putString("backend_url", clean)
        }.apply()

    fun loadOfflineProgress(): OfflineProgress {
        val raw = prefs.getString("offline_progress", null) ?: return OfflineProgress()
        return try {
            val o = JSONObject(raw)
            val mapObj = o.optJSONObject("lessons") ?: JSONObject()
            val map = buildMap {
                val keys = mapObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    k.toIntOrNull()?.let { put(it, mapObj.getInt(k)) }
                }
            }
            OfflineProgress(
                xp = o.optInt("xp", 0),
                streak = o.optInt("streak", 0),
                lastDay = o.optString("lastDay", ""),
                bestAccByLesson = map,
            )
        } catch (_: Exception) {
            OfflineProgress()
        }
    }

    fun saveOfflineProgress(progress: OfflineProgress) {
        val lessonsJson = JSONObject()
        progress.bestAccByLesson.forEach { (id, acc) -> lessonsJson.put(id.toString(), acc) }
        val root = JSONObject()
            .put("xp", progress.xp)
            .put("streak", progress.streak)
            .put("lastDay", progress.lastDay)
            .put("lessons", lessonsJson)
        prefs.edit().putString("offline_progress", root.toString()).apply()
    }
}
