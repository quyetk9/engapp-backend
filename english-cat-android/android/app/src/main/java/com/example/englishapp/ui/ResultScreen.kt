package com.example.englishapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishapp.domain.CompleteResult

@Composable
fun ResultScreen(result: CompleteResult, isReviewOnly: Boolean = false, onContinue: () -> Unit) {
    val time = "%d:%02d".format(result.seconds / 60, result.seconds % 60)
    val (mascot, headline) = when {
        result.accuracy >= 100 -> "🏆🐱" to "Bài học hoàn hảo, không sai lỗi nào!"
        result.accuracy >= 80 -> "💪🐱" to "Bạn tiến bộ thần tốc!"
        else -> "😺" to "Hoàn thành bài học! Tiếp tục luyện nhé!"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(mascot, fontSize = 88.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            headline,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp,
        )
        Spacer(Modifier.height(24.dp))

        StatBox("🎯 ĐỘ CHÍNH XÁC", "${result.accuracy}%", Amber, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox("THỜI GIAN", time, Blue, Modifier.weight(1f))
            StatBox("⚡ XP", if (isReviewOnly) "Ôn tập" else "+${result.xpGained}", Green, Modifier.weight(1f))
            StatBox("TỪ VỰNG", "${result.newWords}", Pink, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        if (!isReviewOnly) {
            Text(
                "🔥 Chuỗi ${result.streak} ngày  •  Tổng ${result.totalXp} XP",
                color = Color.Gray,
                fontSize = 16.sp,
            )
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton("Tiếp tục", onClick = onContinue)
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}
