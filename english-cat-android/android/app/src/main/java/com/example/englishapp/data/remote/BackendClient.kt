package com.example.englishapp.data.remote

import com.example.englishapp.BuildConfig
import com.example.englishapp.domain.CompleteResult
import com.example.englishapp.domain.Exercise
import com.example.englishapp.domain.LessonDetail
import com.example.englishapp.domain.LessonSummary
import com.example.englishapp.domain.MatchPair
import com.example.englishapp.domain.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiException(message: String, val code: Int) : Exception(message)

/**
 * HTTP client thuần giao tiếp với Backend tách rời. Có thể thay đổi baseUrl khi runtime.
 */
class BackendClient(initialBaseUrl: String = BuildConfig.API_BASE_URL) {
    var baseUrl: String = normalizeBaseUrl(initialBaseUrl)
        set(value) {
            field = normalizeBaseUrl(value)
        }

    var token: String? = null

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().ifEmpty { BuildConfig.API_BASE_URL }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val cleanPath = path.trimStart('/')
            val conn = URL(baseUrl + cleanPath).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 20_000
                conn.readTimeout = 35_000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = if (text.isBlank()) {
                    JSONObject()
                } else {
                    try {
                        JSONObject(text)
                    } catch (_: JSONException) {
                        if (code !in 200..299) {
                            throw ApiException("Máy chủ đang phản hồi lỗi (HTTP $code). Vui lòng thử lại.", code)
                        }
                        throw ApiException("Dữ liệu máy chủ trả về không hợp lệ.", code)
                    }
                }
                if (code !in 200..299) {
                    throw ApiException(json.optString("error", "Lỗi máy chủ ($code)"), code)
                }
                json
            } finally {
                conn.disconnect()
            }
        }

    suspend fun register(name: String, email: String, password: String): Pair<User, String> {
        val r = request(
            "POST", "api/auth/register",
            JSONObject().put("name", name).put("email", email).put("password", password)
        )
        return parseUser(r.getJSONObject("user")) to r.getString("token")
    }

    suspend fun login(email: String, password: String): Pair<User, String> {
        val r = request("POST", "api/auth/login", JSONObject().put("email", email).put("password", password))
        return parseUser(r.getJSONObject("user")) to r.getString("token")
    }

    suspend fun me(): User = parseUser(request("GET", "api/me").getJSONObject("user"))

    suspend fun lessons(): List<LessonSummary> {
        val arr = request("GET", "api/lessons").getJSONArray("lessons")
        return List(arr.length()) { parseLessonSummary(arr.getJSONObject(it)) }
    }

    suspend fun lesson(id: Int): LessonDetail =
        parseLessonDetail(request("GET", "api/lessons/$id").getJSONObject("lesson"))

    suspend fun complete(id: Int, correct: Int, total: Int, seconds: Int): CompleteResult =
        parseCompleteResult(
            request(
                "POST", "api/lessons/$id/complete",
                JSONObject().put("correct", correct).put("total", total).put("seconds", seconds)
            )
        )
}

private fun JSONArray.toStringList(): List<String> = List(length()) { getString(it) }

private fun parseUser(o: JSONObject) = User(
    id = o.getInt("id"),
    name = o.getString("name"),
    email = o.getString("email"),
    xp = o.getInt("xp"),
    streak = o.getInt("streak"),
)

private fun parseLessonSummary(o: JSONObject) = LessonSummary(
    id = o.getInt("id"),
    title = o.getString("title"),
    totalExercises = o.getInt("totalExercises"),
    completed = o.getBoolean("completed"),
    bestAccuracy = o.getInt("bestAccuracy"),
    chapterTitle = o.optString("chapterTitle", o.optString("chapter", "")),
    wordCount = o.optInt("wordCount", 0),
)

private fun parseExercise(o: JSONObject): Exercise = when (val type = o.getString("type")) {
    "card" -> Exercise.Card(
        word = o.getString("word"),
        ipa = o.optString("ipa"),
        meaningVi = o.getString("meaningVi"),
        emoji = o.optString("emoji"),
        partOfSpeech = o.optString("partOfSpeech", o.optString("pos", "")),
        note = o.optString("note", o.optString("explanation", "")),
    )

    "tip", "note" -> Exercise.Tip(
        title = o.optString("title", "Lưu ý!"),
        body = o.optString("body", o.optString("text", o.optString("message", ""))),
    )

    "match" -> {
        val arr = o.getJSONArray("pairs")
        Exercise.Match(
            List(arr.length()) {
                val p = arr.getJSONObject(it)
                MatchPair(p.getString("word"), p.optString("emoji"))
            }
        )
    }

    "fill" -> Exercise.Fill(
        promptVi = o.getString("promptVi"),
        template = o.getString("template"),
        answers = o.getJSONArray("answers").toStringList(),
        options = o.getJSONArray("options").toStringList(),
    )

    "listen" -> Exercise.Listen(
        promptVi = o.getString("promptVi"),
        options = o.getJSONArray("options").toStringList(),
        answerIndex = o.getInt("answerIndex"),
    )

    "order" -> Exercise.Order(
        promptVi = o.getString("promptVi"),
        answer = o.getJSONArray("answer").toStringList(),
        words = o.getJSONArray("words").toStringList(),
    )

    "choice" -> Exercise.Choice(
        promptHeader = o.optString("promptHeader", "Dịch câu này"),
        promptText = o.optString("promptText", o.optString("promptVi", "")),
        options = o.getJSONArray("options").toStringList(),
        answerIndex = o.getInt("answerIndex"),
        horizontal = o.optBoolean("horizontal", false),
    )

    "true_false", "trueFalse" -> Exercise.TrueFalse(
        statement = o.optString("statement", o.optString("promptVi", "")),
        answer = o.getBoolean("answer"),
        explanation = o.optString("explanation", ""),
    )

    else -> Exercise.Tip(
        title = "Nội dung ($type)",
        body = o.optString("promptVi", "Dạng bài mới chưa được hỗ trợ trên phiên bản này."),
    )
}

private fun parseLessonDetail(o: JSONObject): LessonDetail {
    val arr = o.getJSONArray("exercises")
    return LessonDetail(
        id = o.getInt("id"),
        title = o.getString("title"),
        exercises = List(arr.length()) { parseExercise(arr.getJSONObject(it)) },
    )
}

private fun parseCompleteResult(o: JSONObject) = CompleteResult(
    xpGained = o.getInt("xpGained"),
    totalXp = o.getInt("totalXp"),
    streak = o.getInt("streak"),
    accuracy = o.getInt("accuracy"),
    seconds = o.getInt("seconds"),
    newWords = o.getInt("newWords"),
)
