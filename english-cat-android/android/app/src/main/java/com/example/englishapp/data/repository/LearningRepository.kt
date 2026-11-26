package com.example.englishapp.data.repository

import com.example.englishapp.data.local.LocalCurriculum
import com.example.englishapp.data.local.SessionStore
import com.example.englishapp.data.local.VocabStore
import com.example.englishapp.data.remote.ApiException
import com.example.englishapp.data.remote.BackendClient
import com.example.englishapp.domain.CompleteResult
import com.example.englishapp.domain.Exercise
import com.example.englishapp.domain.LessonDetail
import com.example.englishapp.domain.LessonSummary
import com.example.englishapp.domain.User
import com.example.englishapp.domain.VocabItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Tầng Repository tách biệt hoàn toàn UI khỏi chi tiết Backend (Online) hay Local (Offline).
 */
class LearningRepository(
    private val sessionStore: SessionStore,
    private val vocabStore: VocabStore,
    private val backend: BackendClient = BackendClient(sessionStore.backendUrl),
) {
    init {
        backend.token = sessionStore.token
    }

    val isOfflineMode: Boolean
        get() = sessionStore.offlineMode

    val hasActiveSession: Boolean
        get() = sessionStore.offlineMode || sessionStore.token != null

    var backendUrl: String
        get() = sessionStore.backendUrl
        set(value) {
            sessionStore.backendUrl = value
            backend.baseUrl = sessionStore.backendUrl
        }

    fun startOfflineSession() {
        sessionStore.offlineMode = true
        sessionStore.token = null
        backend.token = null
    }

    fun logout() {
        sessionStore.offlineMode = false
        sessionStore.token = null
        backend.token = null
    }

    suspend fun login(email: String, password: String): User {
        val (user, token) = backend.login(email, password)
        sessionStore.offlineMode = false
        sessionStore.token = token
        backend.token = token
        return user
    }

    suspend fun register(name: String, email: String, password: String): User {
        val (user, token) = backend.register(name, email, password)
        sessionStore.offlineMode = false
        sessionStore.token = token
        backend.token = token
        return user
    }

    suspend fun loadHomeState(): Pair<User, List<LessonSummary>> {
        if (sessionStore.offlineMode) {
            val prog = sessionStore.loadOfflineProgress()
            val user = User(
                id = 0,
                name = "Học viên Offline",
                email = "offline@local",
                xp = prog.xp,
                streak = prog.streak,
            )
            val lessons = LocalCurriculum.lessons.map { l ->
                val best = prog.bestAccByLesson[l.id]
                LessonSummary(
                    id = l.id,
                    title = l.title,
                    totalExercises = l.exercises.size,
                    completed = best != null,
                    bestAccuracy = best ?: 0,
                    chapterTitle = l.chapterTitle,
                    wordCount = l.wordCount,
                )
            }
            return user to lessons
        }
        return backend.me() to backend.lessons()
    }

    suspend fun openLesson(userId: Int?, lessonId: Int): LessonDetail {
        val detail = if (sessionStore.offlineMode) {
            LocalCurriculum.findDetail(lessonId)
                ?: throw IllegalStateException("Không tìm thấy bài học #$lessonId.")
        } else {
            backend.lesson(lessonId)
        }
        vocabStore.recordFromLesson(userId, detail.exercises)
        return detail
    }

    suspend fun completeLesson(
        lessonId: Int,
        correct: Int,
        total: Int,
        seconds: Int,
    ): CompleteResult {
        if (sessionStore.offlineMode) {
            return completeOffline(lessonId, correct, total, seconds)
        }
        return backend.complete(lessonId, correct, total, seconds)
    }

    private fun completeOffline(lessonId: Int, correct: Int, total: Int, seconds: Int): CompleteResult {
        val safeTotal = total.coerceAtLeast(1)
        val safeCorrect = correct.coerceIn(0, safeTotal)
        val accuracy = ((safeCorrect * 100f) / safeTotal).toInt().coerceIn(0, 100)
        val xpGained = 10 + safeCorrect * 5 + if (accuracy == 100) 10 else 0

        val cur = sessionStore.loadOfflineProgress()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val today = fmt.format(Date())
        val yesterday = fmt.format(Date(System.currentTimeMillis() - 86_400_000L))
        val newStreak = when (cur.lastDay) {
            today -> cur.streak.coerceAtLeast(1)
            yesterday -> cur.streak + 1
            else -> 1
        }
        val prevBest = cur.bestAccByLesson[lessonId]
        val isFirstComplete = prevBest == null
        val updatedMap = cur.bestAccByLesson.toMutableMap().apply {
            put(lessonId, maxOf(prevBest ?: 0, accuracy))
        }
        val newTotalXp = cur.xp + xpGained
        sessionStore.saveOfflineProgress(
            cur.copy(
                xp = newTotalXp,
                streak = newStreak,
                lastDay = today,
                bestAccByLesson = updatedMap,
            )
        )
        val lessonWords = LocalCurriculum.lessons.find { it.id == lessonId }?.wordCount ?: 0
        return CompleteResult(
            xpGained = xpGained,
            totalXp = newTotalXp,
            streak = newStreak,
            accuracy = accuracy,
            seconds = seconds.coerceAtLeast(1),
            newWords = if (isFirstComplete) lessonWords else 0,
        )
    }

    fun vocabList(userId: Int?): List<VocabItem> = vocabStore.list(userId)

    fun recordVocabAnswer(userId: Int?, ex: Exercise, isCorrect: Boolean) {
        when (ex) {
            is Exercise.Choice -> vocabStore.updateWordStats(userId, ex.promptText, isCorrect)
            is Exercise.Listen -> ex.options.getOrNull(ex.answerIndex)?.let {
                vocabStore.updateWordStats(userId, it, isCorrect)
            }
            is Exercise.Match -> ex.pairs.forEach {
                vocabStore.updateWordStats(userId, it.word, isCorrect)
            }
            else -> Unit
        }
    }

    fun buildVocabReviewLesson(userId: Int?): LessonDetail? = vocabStore.buildReviewLesson(userId)

    fun formatError(e: Exception): String =
        when (e) {
            is ApiException -> e.message ?: "Có lỗi từ máy chủ"
            else -> "Không kết nối được máy chủ (${backend.baseUrl}). Bạn có thể thử lại hoặc chuyển sang chế độ Offline."
        }

    fun isUnauthorized(e: Exception): Boolean = e is ApiException && e.code == 401
}
