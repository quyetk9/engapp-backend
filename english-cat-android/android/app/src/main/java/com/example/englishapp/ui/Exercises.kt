package com.example.englishapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishapp.domain.Exercise
import com.example.englishapp.domain.resolvedPartOfSpeech

@Composable
fun CardExercise(card: Exercise.Card, speak: (String) -> Unit) {
    LaunchedEffect(card) { speak(card.word) }
    val pos = remember(card) { card.resolvedPartOfSpeech() }
    val explanation = remember(card) {
        buildString {
            append("**${card.word}** có nghĩa là ‘${card.meaningVi}’.")
            if (card.note.isNotBlank()) {
                append(" ")
                append(card.note)
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.Black,
            border = BorderStroke(3.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(1.dp).background(Outline))
                    Text(
                        pos,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(Outline))
                }
                Spacer(Modifier.height(14.dp))
                Text(card.word, color = Blue, fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (card.ipa.isNotBlank()) {
                    Text("/${card.ipa}/", color = Color.Gray, fontSize = 20.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    highlightStyledText(explanation),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                )
                if (card.emoji.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(card.emoji, fontSize = 88.sp)
                }
                Spacer(Modifier.height(12.dp))
                Chip("🔊 Nghe lại", onClick = { speak(card.word) })
            }
        }
    }
}

@Composable
fun TipExercise(tip: Exercise.Tip) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.Black,
            border = BorderStroke(3.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(Blue),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✦", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Text(tip.title, color = Blue, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(
                    highlightStyledText(tip.body),
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                )
            }
        }
        Row(Modifier.fillMaxWidth(0.55f), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.width(10.dp).height(90.dp).background(CardBorder))
            Box(Modifier.width(10.dp).height(90.dp).background(CardBorder))
        }
    }
}

@Composable
fun MatchExercise(
    ex: Exercise.Match,
    hintSignal: Int,
    speak: (String) -> Unit,
    onAnswer: (Boolean) -> Unit,
) {
    val n = ex.pairs.size
    val pool = remember(ex) { ex.pairs.shuffled() }
    val slots = remember(ex) { mutableStateListOf<Int?>().apply { repeat(n) { add(null) } } }
    var verdict by remember(ex) { mutableStateOf<Boolean?>(null) }
    val locked = verdict != null

    fun evaluateIfFull() {
        if (slots.none { it == null }) {
            val ok = (0 until n).all { k -> pool[slots[k]!!].word == ex.pairs[k].word }
            verdict = ok
            onAnswer(ok)
        }
    }

    LaunchedEffect(hintSignal) {
        if (hintSignal > 0 && !locked) {
            val targetSlot = (0 until n).firstOrNull { k ->
                val idx = slots[k]
                idx == null || pool[idx].word != ex.pairs[k].word
            }
            if (targetSlot != null) {
                val neededWord = ex.pairs[targetSlot].word
                val rightPoolIdx = pool.indexOfFirst { it.word == neededWord }
                if (rightPoolIdx >= 0) {
                    val prevHolder = slots.indexOfFirst { it == rightPoolIdx }
                    if (prevHolder >= 0) slots[prevHolder] = null
                    slots[targetSlot] = rightPoolIdx
                    speak(neededWord)
                    evaluateIfFull()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ghép các mục sau", color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ex.pairs.forEach {
                Text(
                    it.word,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until n) {
                val idx = slots[i]
                val boxMod = Modifier.weight(1f).aspectRatio(0.75f)
                if (idx == null) {
                    DashedBox(boxMod)
                } else {
                    val ok = pool[idx].word == ex.pairs[i].word
                    val border = if (!locked) Color.Transparent else if (ok) Green else Red
                    PictureCard(pool[idx].emoji, boxMod, border) { if (!locked) slots[i] = null }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (j in pool.indices) {
                val boxMod = Modifier.weight(1f).aspectRatio(0.75f)
                if (slots.contains(j)) {
                    Spacer(boxMod)
                } else {
                    PictureCard(pool[j].emoji, boxMod, Outline) {
                        if (!locked) {
                            val target = slots.indexOfFirst { it == null }
                            if (target >= 0) {
                                slots[target] = j
                                speak(pool[j].word)
                                evaluateIfFull()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillExercise(ex: Exercise.Fill, hintSignal: Int, onAnswer: (Boolean) -> Unit) {
    val parts = remember(ex) { ex.template.split("___") }
    val blanks = remember(ex) { minOf(ex.answers.size, (parts.size - 1).coerceAtLeast(0)) }
    val slots = remember(ex) { mutableStateListOf<Int?>().apply { repeat(blanks) { add(null) } } }
    var verdict by remember(ex) { mutableStateOf<Boolean?>(null) }
    val locked = verdict != null

    fun evaluateIfFull() {
        if (blanks > 0 && slots.none { it == null }) {
            val ok = (0 until blanks).all { k ->
                ex.options[slots[k]!!].equals(ex.answers[k], ignoreCase = true)
            }
            verdict = ok
            onAnswer(ok)
        }
    }

    LaunchedEffect(hintSignal) {
        if (hintSignal > 0 && !locked && blanks > 0) {
            val targetBlank = (0 until blanks).firstOrNull { k ->
                val s = slots[k]
                s == null || !ex.options[s].equals(ex.answers[k], ignoreCase = true)
            }
            if (targetBlank != null) {
                val rightOpt = ex.options.indices.firstOrNull { idx ->
                    ex.options[idx].equals(ex.answers[targetBlank], ignoreCase = true) && idx !in slots
                } ?: ex.options.indexOfFirst { it.equals(ex.answers[targetBlank], ignoreCase = true) }

                if (rightOpt >= 0) {
                    val prev = slots.indexOfFirst { it == rightOpt }
                    if (prev >= 0) slots[prev] = null
                    slots[targetBlank] = rightOpt
                    evaluateIfFull()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Dịch câu này", color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(ex.promptVi, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        FlowRow(
            Modifier.fillMaxWidth().heightIn(min = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            parts.forEachIndexed { pi, part ->
                part.trim().split(" ").filter { it.isNotEmpty() }.forEach { w ->
                    Text(
                        w,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                if (pi < blanks) {
                    val s = slots[pi]
                    if (s == null) {
                        EmptySlot()
                    } else {
                        val ok = ex.options[s].equals(ex.answers[pi], ignoreCase = true)
                        val tint = if (!locked) Color.White else if (ok) Green else Red
                        val onRemove: (() -> Unit)? = if (locked) null else ({ slots[pi] = null })
                        Chip(
                            ex.options[s],
                            background = if (locked) tint.copy(alpha = 0.15f) else Panel,
                            border = if (locked) tint else Outline,
                            textColor = tint,
                            onClick = onRemove,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(Outline))
        Spacer(Modifier.height(32.dp))

        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ex.options.forEachIndexed { i, option ->
                if (slots.contains(i)) {
                    Chip(option, background = Panel, border = Color.Transparent, textColor = Color.Transparent)
                } else {
                    Chip(option, onClick = {
                        if (!locked) {
                            val target = slots.indexOfFirst { it == null }
                            if (target >= 0) {
                                slots[target] = i
                                evaluateIfFull()
                            }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun ListenExercise(
    ex: Exercise.Listen,
    hintSignal: Int,
    speak: (String) -> Unit,
    onAnswer: (Boolean) -> Unit,
) {
    var chosen by remember(ex) { mutableStateOf<Int?>(null) }
    val eliminated = remember(ex) { mutableStateListOf<Int>() }

    LaunchedEffect(ex) {
        ex.options.getOrNull(ex.answerIndex)?.let { speak(it) }
    }

    LaunchedEffect(hintSignal) {
        if (hintSignal > 0 && chosen == null) {
            val candidate = ex.options.indices.firstOrNull { it != ex.answerIndex && it !in eliminated }
            if (candidate != null) eliminated += candidate
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nghe và chọn đáp án đúng", color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(ex.promptVi, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        ex.options.forEachIndexed { i, text ->
            val picked = chosen
            val isRight = i == ex.answerIndex
            val isEliminated = i in eliminated && picked == null
            val tint = when {
                isEliminated -> Color.DarkGray
                picked == null -> Color.White
                isRight -> Green
                picked == i -> Red
                else -> Color.White
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Panel)
                        .clickable { speak(text) },
                    contentAlignment = Alignment.Center,
                ) { Text("▶", color = if (isEliminated) Color.DarkGray else Blue, fontSize = 26.sp) }
                Spacer(Modifier.width(12.dp))
                Chip(
                    text = if (picked == null) "Lựa chọn ${'A' + i}" else text,
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                    background = if (picked != null && (isRight || picked == i)) tint.copy(alpha = 0.15f) else Color.Transparent,
                    border = if (picked != null && (isRight || picked == i)) tint else if (isEliminated) Color(0xFF1F2225) else Outline,
                    textColor = tint,
                    onClick = if (picked == null && !isEliminated) {
                        {
                            chosen = i
                            onAnswer(isRight)
                        }
                    } else null,
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderExercise(ex: Exercise.Order, hintSignal: Int, onAnswer: (Boolean) -> Unit) {
    val pool = remember(ex) { ex.words.shuffled() }
    val placed = remember(ex) { mutableStateListOf<Int>() }
    var verdict by remember(ex) { mutableStateOf<Boolean?>(null) }
    val locked = verdict != null
    val hasDistractors = ex.words.size > ex.answer.size

    fun submitAnswer() {
        val ok = placed.size == ex.answer.size &&
            placed.indices.all { k -> pool[placed[k]].equals(ex.answer[k], ignoreCase = true) }
        verdict = ok
        onAnswer(ok)
    }

    LaunchedEffect(hintSignal) {
        if (hintSignal > 0 && !locked && placed.size < ex.answer.size) {
            val validPrefixLen = placed.indices.takeWhile { k ->
                k < ex.answer.size && pool[placed[k]].equals(ex.answer[k], ignoreCase = true)
            }.size
            while (placed.size > validPrefixLen) placed.removeAt(placed.lastIndex)

            val targetPos = placed.size
            if (targetPos < ex.answer.size) {
                val expectedWord = ex.answer[targetPos]
                val poolIdx = pool.indices.firstOrNull { j ->
                    j !in placed && pool[j].equals(expectedWord, ignoreCase = true)
                }
                if (poolIdx != null) {
                    placed.add(poolIdx)
                    if (!hasDistractors && placed.size == ex.answer.size) submitAnswer()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Sắp xếp các từ theo đúng thứ tự", color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(ex.promptVi, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(Outline))

        FlowRow(
            Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            placed.forEach { idx ->
                val tint = when (verdict) {
                    null -> Color.White
                    true -> Green
                    false -> Red
                }
                val onRemove: (() -> Unit)? = if (locked) null else ({ placed.removeAll { it == idx } })
                Chip(
                    pool[idx],
                    background = if (locked) tint.copy(alpha = 0.15f) else Color.Transparent,
                    border = if (locked) tint else Outline,
                    textColor = tint,
                    onClick = onRemove,
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(2.dp).background(Outline))
        Spacer(Modifier.height(28.dp))

        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            pool.forEachIndexed { j, word ->
                if (placed.contains(j)) {
                    Chip(word, background = Panel, border = Color.Transparent, textColor = Color.Transparent)
                } else {
                    Chip(word, onClick = {
                        if (!locked) {
                            placed.add(j)
                            if (!hasDistractors && placed.size == ex.answer.size) {
                                submitAnswer()
                            }
                        }
                    })
                }
            }
        }

        if (hasDistractors && !locked && placed.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Kiểm tra", onClick = ::submitAnswer)
        }
    }
}

@Composable
fun ChoiceExercise(
    ex: Exercise.Choice,
    hintSignal: Int,
    speak: (String) -> Unit,
    onAnswer: (Boolean) -> Unit,
) {
    var chosen by remember(ex) { mutableStateOf<Int?>(null) }
    val eliminated = remember(ex) { mutableStateListOf<Int>() }

    LaunchedEffect(hintSignal) {
        if (hintSignal > 0 && chosen == null) {
            val candidate = ex.options.indices.firstOrNull { it != ex.answerIndex && it !in eliminated }
            if (candidate != null) eliminated += candidate
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(ex.promptHeader, color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            ex.promptText,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { speak(ex.promptText) },
        )
        Spacer(Modifier.height(28.dp))

        if (ex.horizontal && ex.options.size == 2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ex.options.forEachIndexed { i, opt ->
                    ChoiceCard(
                        text = opt,
                        chosen = chosen,
                        index = i,
                        answerIndex = ex.answerIndex,
                        eliminated = i in eliminated,
                        modifier = Modifier.weight(1f).heightIn(min = 84.dp),
                        onClick = {
                            if (chosen == null && i !in eliminated) {
                                chosen = i
                                onAnswer(i == ex.answerIndex)
                            }
                        },
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ex.options.forEachIndexed { i, opt ->
                    ChoiceCard(
                        text = opt,
                        chosen = chosen,
                        index = i,
                        answerIndex = ex.answerIndex,
                        eliminated = i in eliminated,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                        onClick = {
                            if (chosen == null && i !in eliminated) {
                                chosen = i
                                onAnswer(i == ex.answerIndex)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    text: String,
    chosen: Int?,
    index: Int,
    answerIndex: Int,
    eliminated: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val isRight = index == answerIndex
    val active = chosen != null && (isRight || chosen == index)
    val tint = when {
        eliminated && chosen == null -> Color.DarkGray
        chosen == null -> Color.White
        isRight -> Green
        chosen == index -> Red
        else -> Color.Gray
    }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (active) tint.copy(alpha = 0.15f) else Panel)
            .border(2.dp, if (active) tint else if (eliminated) Color(0xFF1F2225) else Outline, shape)
            .clickable(enabled = chosen == null && !eliminated, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = tint, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun TrueFalseExercise(
    ex: Exercise.TrueFalse,
    hintSignal: Int,
    onAnswer: (Boolean) -> Unit,
) {
    var chosen by remember(ex) { mutableStateOf<Boolean?>(null) }
    var hintedWrong by remember(ex) { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(hintSignal) {
        if (hintSignal > 0 && chosen == null) {
            hintedWrong = !ex.answer
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Đúng hay sai", color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            highlightStyledText(ex.statement),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
        )
        Spacer(Modifier.height(32.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(true to "✓", false to "✕").forEach { (value, symbol) ->
                val isEliminated = hintedWrong == value && chosen == null
                val isRight = value == ex.answer
                val active = chosen != null && (isRight || chosen == value)
                val tint = when {
                    isEliminated -> Color.DarkGray
                    chosen == null -> Blue
                    isRight -> Green
                    chosen == value -> Red
                    else -> Color.Gray
                }
                val shape = RoundedCornerShape(18.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .height(86.dp)
                        .clip(shape)
                        .background(if (active) tint.copy(alpha = 0.15f) else Color.Black)
                        .border(2.dp, if (active) tint else if (isEliminated) Color(0xFF1F2225) else Outline, shape)
                        .clickable(enabled = chosen == null && !isEliminated) {
                            chosen = value
                            onAnswer(value == ex.answer)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(symbol, color = tint, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
