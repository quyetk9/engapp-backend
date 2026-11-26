package com.example.englishapp.domain

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val xp: Int,
    val streak: Int,
)

data class LessonSummary(
    val id: Int,
    val title: String,
    val totalExercises: Int,
    val completed: Boolean,
    val bestAccuracy: Int,
    val chapterTitle: String = "",
    val wordCount: Int = 0,
)

data class MatchPair(val word: String, val emoji: String)

sealed interface Exercise {
    data class Card(
        val word: String,
        val ipa: String,
        val meaningVi: String,
        val emoji: String,
        val partOfSpeech: String = "",
        val note: String = "",
    ) : Exercise

    data class Tip(
        val title: String,
        val body: String,
    ) : Exercise

    data class Match(val pairs: List<MatchPair>) : Exercise

    data class Fill(
        val promptVi: String,
        val template: String,
        val answers: List<String>,
        val options: List<String>,
    ) : Exercise

    data class Listen(val promptVi: String, val options: List<String>, val answerIndex: Int) : Exercise

    data class Order(val promptVi: String, val answer: List<String>, val words: List<String>) : Exercise

    data class Choice(
        val promptHeader: String,
        val promptText: String,
        val options: List<String>,
        val answerIndex: Int,
        val horizontal: Boolean = false,
    ) : Exercise

    data class TrueFalse(
        val statement: String,
        val answer: Boolean,
        val explanation: String = "",
    ) : Exercise
}

data class LessonDetail(val id: Int, val title: String, val exercises: List<Exercise>)

data class CompleteResult(
    val xpGained: Int,
    val totalXp: Int,
    val streak: Int,
    val accuracy: Int,
    val seconds: Int,
    val newWords: Int,
)

data class VocabItem(
    val word: String,
    val ipa: String,
    val meaningVi: String,
    val emoji: String,
    val partOfSpeech: String,
    val wrongCount: Int = 0,
    val seenCount: Int = 1,
)

/** Bài tập có chấm điểm hay chỉ là thẻ lý thuyết/ghi chú. */
fun Exercise.isGraded(): Boolean = this !is Exercise.Card && this !is Exercise.Tip

/** Bài tập có hỗ trợ nút gợi ý (bóng đèn). */
fun Exercise.supportsHint(): Boolean = isGraded()

/** Đáp án đúng để hiện khi trả lời sai (null = không hiện). */
fun Exercise.solution(): String? = when (this) {
    is Exercise.Fill -> answers.joinToString(" / ")
    is Exercise.Listen -> options.getOrNull(answerIndex)
    is Exercise.Order -> answer.joinToString(" ")
    is Exercise.Choice -> options.getOrNull(answerIndex)
    is Exercise.TrueFalse -> if (answer) "Đúng" else "Sai"
    is Exercise.Match -> pairs.joinToString(", ") { "${it.word} ${it.emoji}" }
    is Exercise.Card, is Exercise.Tip -> null
}

/** Suy luận loại từ nếu nguồn dữ liệu chưa có partOfSpeech. */
fun Exercise.Card.resolvedPartOfSpeech(): String {
    if (partOfSpeech.isNotBlank()) return partOfSpeech.uppercase()
    val w = word.trim()
    val lower = w.lowercase()
    return when {
        lower in setOf("hi", "hello", "hey", "bye", "goodbye", "thanks", "thank you", "please") -> "THÁN TỪ"
        w.contains(" ") -> "CỤM TỪ"
        else -> "TỪ VỰNG"
    }
}
