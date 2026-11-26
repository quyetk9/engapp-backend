package com.example.englishapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishapp.domain.Exercise
import com.example.englishapp.domain.LessonDetail
import com.example.englishapp.domain.isGraded
import com.example.englishapp.domain.solution
import com.example.englishapp.domain.supportsHint

private data class QueueItem(
    val exercise: Exercise,
    val isRetry: Boolean,
    val originalIndex: Int,
)

@Composable
fun LessonScreen(
    lesson: LessonDetail,
    submitting: Boolean = false,
    onAnswerRecorded: (Exercise, Boolean) -> Unit = { _, _ -> },
    onFinished: (correct: Int, total: Int, seconds: Int) -> Unit,
    onExit: () -> Unit,
) {
    if (lesson.exercises.isEmpty()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("😿", fontSize = 72.sp)
            Spacer(Modifier.height(12.dp))
            Text("Bài học này chưa có nội dung.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Quay lại", onClick = onExit)
        }
        return
    }

    val queue = remember(lesson) {
        mutableStateListOf<QueueItem>().apply {
            lesson.exercises.forEachIndexed { i, ex -> add(QueueItem(ex, isRetry = false, originalIndex = i)) }
        }
    }
    var pos by remember(lesson) { mutableIntStateOf(0) }
    var firstTryCorrect by remember(lesson) { mutableIntStateOf(0) }
    val totalGraded = remember(lesson) { lesson.exercises.count { it.isGraded() } }
    var verdict by remember(lesson, pos) { mutableStateOf<Boolean?>(null) }
    var hintSignal by remember(lesson, pos) { mutableIntStateOf(0) }
    val startedAt = remember(lesson) { System.currentTimeMillis() }
    val speak = rememberSpeaker()

    val item = queue[pos]
    val ex = item.exercise

    val onAnswer: (Boolean) -> Unit = { ok ->
        if (verdict == null) {
            verdict = ok
            onAnswerRecorded(ex, ok)
            if (!item.isRetry) {
                if (ok) {
                    firstTryCorrect += 1
                } else if (ex.isGraded()) {
                    queue.add(item.copy(isRetry = true))
                }
            }
        }
    }

    val next: () -> Unit = {
        if (!submitting) {
            if (pos + 1 < queue.size) {
                pos += 1
            } else {
                val seconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(1)
                if (totalGraded == 0) {
                    onFinished(1, 1, seconds)
                } else {
                    onFinished(firstTryCorrect, totalGraded, seconds)
                }
            }
        }
    }

    val done = pos + (if (verdict != null || !ex.isGraded()) 1 else 0)
    val mascot = when (verdict) {
        null -> "🐱"
        true -> "😸"
        false -> "😿"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "✕",
                color = Color.White,
                fontSize = 26.sp,
                modifier = Modifier.clickable(enabled = !submitting, onClick = onExit).padding(8.dp),
            )
            Spacer(Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = { (done.toFloat() / queue.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = if (item.isRetry) Amber else Blue,
                trackColor = Outline,
            )
        }

        if (item.isRetry) {
            Text(
                "Ôn lại câu vừa nhầm",
                color = Amber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
            )
        }

        if (ex !is Exercise.Card && ex !is Exercise.Tip) {
            Text(mascot, fontSize = 56.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
            key(pos, item.isRetry) {
                when (ex) {
                    is Exercise.Card -> CardExercise(ex, speak)
                    is Exercise.Tip -> TipExercise(ex)
                    is Exercise.Match -> MatchExercise(ex, hintSignal, speak, onAnswer)
                    is Exercise.Fill -> FillExercise(ex, hintSignal, onAnswer)
                    is Exercise.Listen -> ListenExercise(ex, hintSignal, speak, onAnswer)
                    is Exercise.Order -> OrderExercise(ex, hintSignal, onAnswer)
                    is Exercise.Choice -> ChoiceExercise(ex, hintSignal, speak, onAnswer)
                    is Exercise.TrueFalse -> TrueFalseExercise(ex, hintSignal, onAnswer)
                }
            }
        }

        val v = verdict
        if (v != null) {
            Text(
                if (v) "Chính xác! 🎉" else "Chưa đúng",
                color = if (v) Green else Red,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            if (!v) {
                if (ex is Exercise.TrueFalse && ex.explanation.isNotBlank()) {
                    Text(
                        highlightStyledText(ex.explanation),
                        color = Color.White,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    ex.solution()?.let {
                        Text("Đáp án: $it", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!ex.isGraded() || v != null) {
                PrimaryButton(
                    text = if (submitting) "Đang lưu..." else "Tiếp",
                    enabled = !submitting,
                    modifier = Modifier.weight(1f),
                    onClick = next,
                )
            } else {
                Spacer(Modifier.weight(1f).height(56.dp))
                if (ex.supportsHint()) {
                    HintButton(onClick = { hintSignal += 1 })
                }
            }
        }
    }
}
