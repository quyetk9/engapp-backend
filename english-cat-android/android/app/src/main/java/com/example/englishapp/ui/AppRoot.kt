package com.example.englishapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishapp.data.local.SessionStore
import com.example.englishapp.data.local.VocabStore
import com.example.englishapp.data.remote.BackendClient
import com.example.englishapp.data.repository.LearningRepository
import com.example.englishapp.domain.CompleteResult
import com.example.englishapp.domain.Exercise
import com.example.englishapp.domain.LessonDetail
import com.example.englishapp.domain.LessonSummary
import com.example.englishapp.domain.User
import kotlinx.coroutines.launch

sealed interface Screen {
    object Loading : Screen
    data class StartupError(val message: String) : Screen
    object Auth : Screen
    object Home : Screen
    object Vocab : Screen
    data class Lesson(val detail: LessonDetail) : Screen
    data class Result(val result: CompleteResult, val isReviewOnly: Boolean = false) : Screen
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val repo = remember {
        LearningRepository(
            sessionStore = SessionStore(context.applicationContext),
            vocabStore = VocabStore(context.applicationContext),
        )
    }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<Screen>(Screen.Loading) }
    var user by remember { mutableStateOf<User?>(null) }
    var lessons by remember { mutableStateOf<List<LessonSummary>>(emptyList()) }
    var vocabVersion by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var submittingLesson by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadingLessonId by remember { mutableStateOf<Int?>(null) }

    val vocabItems = remember(user?.id, vocabVersion) { repo.vocabList(user?.id) }

    suspend fun loadHome() {
        val (u, ls) = repo.loadHomeState()
        user = u
        lessons = ls
    }

    fun logout() {
        repo.logout()
        user = null
        lessons = emptyList()
        error = null
        loadingLessonId = null
        submittingLesson = false
        screen = Screen.Auth
    }

    fun bootstrap() {
        if (!repo.hasActiveSession) {
            screen = Screen.Auth
            return
        }
        screen = Screen.Loading
        scope.launch {
            try {
                loadHome()
                screen = Screen.Home
            } catch (e: Exception) {
                if (repo.isUnauthorized(e)) {
                    repo.logout()
                    screen = Screen.Auth
                    error = repo.formatError(e)
                } else if (repo.isOfflineMode) {
                    screen = Screen.Home
                } else {
                    screen = Screen.StartupError(repo.formatError(e))
                }
            }
        }
    }

    LaunchedEffect(Unit) { bootstrap() }

    BackHandler(enabled = screen is Screen.Lesson || screen is Screen.Result || screen is Screen.Vocab) {
        if (!submittingLesson) screen = if (screen is Screen.Vocab) Screen.Home else Screen.Home
    }

    when (val s = screen) {
        Screen.Loading -> Box(
            Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🐱", fontSize = 64.sp)
                Spacer(Modifier.height(10.dp))
                Text("Đang kết nối...", color = Color.Gray, fontSize = 15.sp)
            }
        }

        is Screen.StartupError -> Column(
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
            Spacer(Modifier.height(14.dp))
            Text(s.message, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Thử lại", onClick = { bootstrap() })
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Học offline (không cần mạng)", onClick = {
                repo.startOfflineSession()
                bootstrap()
            })
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { logout() }) { Text("Đăng xuất", color = Color.Gray) }
        }

        Screen.Auth -> AuthScreen(
            busy = busy,
            error = error,
            backendUrl = repo.backendUrl,
            onChangeUrl = { repo.backendUrl = it },
            onClearError = { error = null },
            onOfflineMode = {
                repo.startOfflineSession()
                bootstrap()
            },
        ) { isRegister, name, email, password ->
            busy = true
            error = null
            scope.launch {
                try {
                    if (isRegister) repo.register(name, email, password) else repo.login(email, password)
                    loadHome()
                    screen = Screen.Home
                } catch (e: Exception) {
                    error = repo.formatError(e)
                }
                busy = false
            }
        }

        Screen.Home -> HomeScreen(
            user = user,
            isOffline = repo.isOfflineMode,
            lessons = lessons,
            vocabCount = vocabItems.size,
            message = error,
            loadingLessonId = loadingLessonId,
            refreshing = refreshing,
            onRefresh = {
                if (refreshing || repo.isOfflineMode) return@HomeScreen
                error = null
                refreshing = true
                scope.launch {
                    try { loadHome() } catch (e: Exception) {
                        if (repo.isUnauthorized(e)) logout() else error = repo.formatError(e)
                    }
                    refreshing = false
                }
            },
            onOpenLesson = { id ->
                if (loadingLessonId != null) return@HomeScreen
                error = null
                loadingLessonId = id
                scope.launch {
                    try {
                        val detail = repo.openLesson(user?.id, id)
                        vocabVersion += 1
                        screen = Screen.Lesson(detail)
                    } catch (e: Exception) {
                        if (repo.isUnauthorized(e)) logout() else error = repo.formatError(e)
                    }
                    loadingLessonId = null
                }
            },
            onOpenVocab = { error = null; screen = Screen.Vocab },
            onLogout = { logout() },
        )

        Screen.Vocab -> VocabScreen(
            items = vocabItems,
            onStartReview = {
                val review = repo.buildVocabReviewLesson(user?.id)
                if (review != null) screen = Screen.Lesson(review)
            },
            onBack = { screen = Screen.Home },
        )

        is Screen.Lesson -> LessonScreen(
            lesson = s.detail,
            submitting = submittingLesson,
            onAnswerRecorded = { ex, ok ->
                repo.recordVocabAnswer(user?.id, ex, ok)
                vocabVersion += 1
            },
            onExit = {
                if (!submittingLesson) screen = Screen.Home
            },
            onFinished = { correct, total, seconds ->
                if (submittingLesson) return@LessonScreen
                submittingLesson = true
                scope.launch {
                    try {
                        val r = repo.completeLesson(s.detail.id, correct, total, seconds)
                        if (!repo.isOfflineMode) loadHome()
                        screen = Screen.Result(r, isReviewOnly = s.detail.id < 0)
                    } catch (e: Exception) {
                        if (repo.isUnauthorized(e)) logout()
                        else { error = repo.formatError(e); screen = Screen.Home }
                    } finally {
                        submittingLesson = false
                    }
                }
            },
        )

        is Screen.Result -> ResultScreen(
            result = s.result,
            isReviewOnly = s.isReviewOnly,
        ) { screen = if (s.isReviewOnly) Screen.Vocab else Screen.Home }
    }
}
