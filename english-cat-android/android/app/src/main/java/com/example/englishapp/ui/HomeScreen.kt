package com.example.englishapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishapp.domain.LessonSummary
import com.example.englishapp.domain.User
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    user: User?,
    isOffline: Boolean,
    lessons: List<LessonSummary>,
    vocabCount: Int,
    message: String?,
    loadingLessonId: Int?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    onOpenVocab: () -> Unit,
    onLogout: () -> Unit,
) {
    val nextId = lessons.firstOrNull { !it.completed }?.id
    val completedCount = lessons.count { it.completed }
    val chapters = remember(lessons) {
        lessons.groupBy { it.chapterTitle.ifBlank { "Lộ trình cơ bản" } }.entries.toList()
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val nextChapterItemIndex = remember(chapters, nextId) {
        if (nextId == null) -1
        else {
            var offset = 0
            var found = -1
            for ((_, items) in chapters) {
                if (items.any { it.id == nextId }) { found = offset; break }
                offset += 1 + ((items.size + 2) / 3)
            }
            found
        }
    }
    val showJumpToCurrent by remember {
        derivedStateOf { nextChapterItemIndex >= 0 && listState.firstVisibleItemIndex > nextChapterItemIndex + 1 }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Xin chào, ${user?.name ?: ""}!",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isOffline) {
                        Text("Chế độ Offline", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onLogout) {
                    Text(if (isOffline) "Đăng nhập" else "Đăng xuất", color = Color.Gray)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                StatPill("🇺🇸", "EN")
                StatPill("🔥", "${user?.streak ?: 0}")
                StatPill("⚡", "${user?.xp ?: 0}")
            }

            val bannerTitle = when {
                lessons.isEmpty() -> "Sẵn sàng bắt đầu!"
                nextId == null -> "Xuất sắc! Hoàn thành $completedCount/${lessons.size} bài"
                (user?.streak ?: 0) > 0 -> "Giữ vững chuỗi ${user?.streak} ngày!"
                else -> "Học bài tiếp theo ngay"
            }
            val bannerSub = when {
                lessons.isEmpty() -> "Chưa tải được danh sách bài học."
                nextId == null -> "Bạn đã hoàn thành toàn bộ lộ trình. Hãy ôn thêm từ vựng nhé!"
                else -> "Hoàn thành thêm một bài học để tích lũy XP và từ vựng mới."
            }
            Text(bannerTitle, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(bannerSub, color = Color.Gray, fontSize = 15.sp)

            if (message != null) {
                Text(message, color = Red, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (lessons.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("🐱", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Chưa có bài học nào", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (!isOffline) {
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            text = if (refreshing) "Đang tải..." else "Tải lại",
                            enabled = !refreshing,
                            onClick = onRefresh,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    chapters.forEach { (chapterTitle, chapterLessons) ->
                        item(key = "header_$chapterTitle") {
                            ChapterHeader(chapterTitle, chapterLessons)
                        }
                        val rows = chapterLessons.chunked(3)
                        items(rows, key = { row -> "row_${row.first().id}" }) { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                for (col in 0 until 3) {
                                    val lesson = row.getOrNull(col)
                                    if (lesson == null) {
                                        Spacer(Modifier.weight(1f))
                                    } else {
                                        LessonNode(
                                            lesson = lesson,
                                            isNext = lesson.id == nextId,
                                            loading = loadingLessonId == lesson.id,
                                            enabled = loadingLessonId == null,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onOpenLesson(lesson.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showJumpToCurrent) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Blue)
                        .clickable { scope.launch { listState.animateScrollToItem(nextChapterItemIndex) } }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text("↓ Quay lại hiện tại", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(Panel)
                .border(1.dp, Outline)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavTab(icon = "📘", label = "Bài học", active = true, onClick = {})
            BottomNavTab(
                icon = "Aa",
                label = if (vocabCount > 0) "Từ vựng ($vocabCount)" else "Từ vựng",
                active = false,
                onClick = onOpenVocab,
            )
            if (!isOffline) {
                BottomNavTab(
                    icon = "↻",
                    label = if (refreshing) "Đang tải" else "Làm mới",
                    active = false,
                    onClick = { if (!refreshing) onRefresh() },
                )
            }
        }
    }
}

@Composable
private fun ChapterHeader(title: String, items: List<LessonSummary>) {
    val totalWords = items.sumOf { it.wordCount }
    val done = items.count { it.completed }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NavyBg)
            .border(1.dp, Outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        val meta = buildString {
            append("${items.size} chủ đề • Hoàn thành $done/${items.size}")
            if (totalWords > 0) append(" • $totalWords từ")
        }
        Text(meta, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
private fun LessonNode(
    lesson: LessonSummary,
    isNext: Boolean,
    loading: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = when {
        isNext -> Blue
        lesson.completed -> Green
        else -> Color.Gray
    }
    val circleBg = when {
        isNext -> Blue.copy(alpha = 0.18f)
        lesson.completed -> Color(0xFF0B2530)
        else -> Panel
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(circleBg)
                .border(if (isNext) 3.dp else 2.dp, if (isNext) Blue else accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val symbol = when { loading -> "…"; lesson.completed -> "✓"; else -> "▶" }
            Text(symbol, color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            lesson.title,
            color = if (isNext) Blue else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (lesson.completed) "${lesson.bestAccuracy}%" else "${lesson.totalExercises} bài",
            color = Color.Gray,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun StatPill(icon: String, text: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, Outline, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomNavTab(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, color = if (active) Blue else Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (active) Blue else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
