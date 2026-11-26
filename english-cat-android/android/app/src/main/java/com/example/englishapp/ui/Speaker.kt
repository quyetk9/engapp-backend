package com.example.englishapp.ui

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun rememberSpeaker(): (String) -> Unit {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var pendingText by remember { mutableStateOf<String?>(null) }

    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) ready = true
        }
    }

    LaunchedEffect(ready) {
        if (ready) {
            tts.language = Locale.US
            pendingText?.let { text ->
                pendingText = null
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    return { text ->
        if (text.isNotBlank()) {
            if (ready) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt")
            } else {
                pendingText = text
            }
        }
    }
}
