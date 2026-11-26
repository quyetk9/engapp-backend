package com.example.englishapp.data.local

import android.content.Context
import com.example.englishapp.domain.Exercise
import com.example.englishapp.domain.LessonDetail
import com.example.englishapp.domain.MatchPair
import com.example.englishapp.domain.VocabItem
import com.example.englishapp.domain.resolvedPartOfSpeech
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lưu sổ tay từ vựng đã học trên thiết bị và tạo bài ôn tập nhanh (tương tự tab Aa).
 */
class VocabStore(context: Context) {
    private val prefs = context.getSharedPreferences("vocab_bank", Context.MODE_PRIVATE)

    private fun keyFor(userId: Int?): String = "words_${userId ?: 0}"

    fun list(userId: Int?): List<VocabItem> {
        val raw = prefs.getString(keyFor(userId), null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                VocabItem(
                    word = o.getString("word"),
                    ipa = o.optString("ipa"),
                    meaningVi = o.optString("meaningVi"),
                    emoji = o.optString("emoji"),
                    partOfSpeech = o.optString("partOfSpeech"),
                    wrongCount = o.optInt("wrongCount", 0),
                    seenCount = o.optInt("seenCount", 1),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(userId: Int?, items: List<VocabItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("word", item.word)
                    .put("ipa", item.ipa)
                    .put("meaningVi", item.meaningVi)
                    .put("emoji", item.emoji)
                    .put("partOfSpeech", item.partOfSpeech)
                    .put("wrongCount", item.wrongCount)
                    .put("seenCount", item.seenCount)
            )
        }
        prefs.edit().putString(keyFor(userId), arr.toString()).apply()
    }

    fun recordFromLesson(userId: Int?, exercises: List<Exercise>) {
        val map = list(userId).associateBy { it.word.lowercase() }.toMutableMap()
        var changed = false
        exercises.forEach { ex ->
            when (ex) {
                is Exercise.Card -> {
                    val k = ex.word.trim().lowercase()
                    if (k.isNotEmpty()) {
                        val existing = map[k]
                        map[k] = if (existing == null) {
                            VocabItem(
                                word = ex.word.trim(),
                                ipa = ex.ipa,
                                meaningVi = ex.meaningVi,
                                emoji = ex.emoji,
                                partOfSpeech = ex.resolvedPartOfSpeech(),
                            )
                        } else {
                            existing.copy(
                                ipa = ex.ipa.ifBlank { existing.ipa },
                                meaningVi = ex.meaningVi.ifBlank { existing.meaningVi },
                                emoji = ex.emoji.ifBlank { existing.emoji },
                                partOfSpeech = ex.resolvedPartOfSpeech(),
                                seenCount = existing.seenCount + 1,
                            )
                        }
                        changed = true
                    }
                }
                is Exercise.Match -> {
                    ex.pairs.forEach { pair ->
                        val k = pair.word.trim().lowercase()
                        if (k.isNotEmpty() && !map.containsKey(k)) {
                            map[k] = VocabItem(
                                word = pair.word.trim(),
                                ipa = "",
                                meaningVi = "",
                                emoji = pair.emoji,
                                partOfSpeech = "TỪ VỰNG",
                            )
                            changed = true
                        }
                    }
                }
                else -> Unit
            }
        }
        if (changed) save(userId, map.values.toList())
    }

    fun updateWordStats(userId: Int?, word: String, isCorrect: Boolean) {
        val items = list(userId).toMutableList()
        val idx = items.indexOfFirst { it.word.equals(word, ignoreCase = true) }
        if (idx >= 0) {
            val cur = items[idx]
            items[idx] = cur.copy(
                wrongCount = if (isCorrect) (cur.wrongCount - 1).coerceAtLeast(0) else cur.wrongCount + 1,
                seenCount = cur.seenCount + 1,
            )
            save(userId, items)
        }
    }

    fun buildReviewLesson(userId: Int?): LessonDetail? {
        val all = list(userId).filter { it.meaningVi.isNotBlank() }
        if (all.isEmpty()) return null

        val prioritized = all.sortedWith(
            compareByDescending<VocabItem> { it.wrongCount }.thenBy { it.seenCount }
        ).take(5)

        val exercises = mutableListOf<Exercise>()
        val allMeanings = all.map { it.meaningVi }.distinct()
        val allWords = all.map { it.word }.distinct()

        prioritized.forEachIndexed { idx, item ->
            if (idx == 0 || item.wrongCount > 0) {
                exercises += Exercise.Card(
                    word = item.word,
                    ipa = item.ipa,
                    meaningVi = item.meaningVi,
                    emoji = item.emoji,
                    partOfSpeech = item.partOfSpeech,
                    note = if (item.wrongCount > 0) "Từ này bạn từng nhầm, hãy ôn lại nhé!" else "",
                )
            }

            val wrongMeanings = allMeanings.filter { !it.equals(item.meaningVi, ignoreCase = true) }.shuffled()
            if (wrongMeanings.isNotEmpty()) {
                val options = (listOf(item.meaningVi) + wrongMeanings.take(2)).shuffled()
                exercises += Exercise.Choice(
                    promptHeader = "Dịch sang tiếng Việt",
                    promptText = item.word,
                    options = options,
                    answerIndex = options.indexOf(item.meaningVi),
                    horizontal = options.size == 2 && options.all { it.length <= 16 },
                )
            }

            val wrongWords = allWords.filter { !it.equals(item.word, ignoreCase = true) }.shuffled()
            if (wrongWords.isNotEmpty()) {
                val options = (listOf(item.word) + wrongWords.take(2)).shuffled()
                exercises += Exercise.Listen(
                    promptVi = "Nghe và chọn từ có nghĩa là \"${item.meaningVi}\"",
                    options = options,
                    answerIndex = options.indexOf(item.word),
                )
            }

            if (idx % 2 == 1 && wrongMeanings.isNotEmpty()) {
                val showCorrect = (idx % 4 == 1)
                val shownMeaning = if (showCorrect) item.meaningVi else wrongMeanings.first()
                exercises += Exercise.TrueFalse(
                    statement = "**${item.word}** có nghĩa là ‘$shownMeaning’.",
                    answer = showCorrect,
                    explanation = "**${item.word}** nghĩa là ‘${item.meaningVi}’.",
                )
            }
        }

        val matchCandidates = prioritized.filter { it.emoji.isNotBlank() }.take(3)
        if (matchCandidates.size >= 2) {
            exercises += Exercise.Match(matchCandidates.map { MatchPair(it.word, it.emoji) })
        }

        return LessonDetail(id = -1, title = "Luyện từ vựng", exercises = exercises)
    }
}
