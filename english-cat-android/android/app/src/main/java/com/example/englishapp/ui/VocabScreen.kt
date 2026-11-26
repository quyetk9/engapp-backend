package com.example.englishapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishapp.domain.VocabItem

@Composable
fun VocabScreen(
    items: List<VocabItem>,
    onStartReview: () -> Unit,
    onBack: () -> Unit,
) {
    val speak = rememberSpeaker()
    val reviewableCount = items.count { it.meaningVi.isNotBlank() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("←", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Sổ từ vựng", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${items.size} từ đã gặp", color = Color.Gray, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("📖", fontSize = 68.sp)
                Spacer(Modifier.height(12.dp))
                Text("Chưa có từ vựng nào được lưu", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Hãy mở và học một bài học để tự động lưu từ vựng vào đây.", color = Color.Gray, fontSize = 15.sp)
            }
        } else {
            if (reviewableCount > 0) {
                PrimaryButton("Luyện từ vựng ngay ⚡", onClick = onStartReview)
                Spacer(Modifier.height(16.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(items, key = { it.word.lowercase() }) { item ->
                    VocabRow(item = item, onSpeak = { speak(item.word) })
                }
            }
        }
    }
}

@Composable
private fun VocabRow(item: VocabItem, onSpeak: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel)
            .border(1.dp, Outline, shape)
            .clickable(onClick = onSpeak)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(NavyBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.emoji.ifBlank { "🔤" }, fontSize = 26.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.word, color = Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (item.ipa.isNotBlank()) Text("/${item.ipa}/", color = Color.Gray, fontSize = 13.sp)
                if (item.wrongCount > 0) Text("Cần ôn", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (item.meaningVi.isNotBlank()) Text(item.meaningVi, color = Color.White, fontSize = 15.sp)
        }
        Text("🔊", fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
    }
}
