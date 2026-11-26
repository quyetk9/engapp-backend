package com.example.englishapp.data.local

import com.example.englishapp.domain.Exercise
import com.example.englishapp.domain.LessonDetail
import com.example.englishapp.domain.MatchPair

data class LocalLessonRecord(
    val id: Int,
    val chapterTitle: String,
    val title: String,
    val wordCount: Int,
    val exercises: List<Exercise>,
)

/**
 * Chương trình học tích hợp sẵn để app chạy độc lập hoàn toàn khi không có backend.
 */
object LocalCurriculum {
    val lessons: List<LocalLessonRecord> = listOf(
        LocalLessonRecord(
            id = 1,
            chapterTitle = "Chào hỏi và giao tiếp cơ bản",
            title = "Bắt đầu nào",
            wordCount = 4,
            exercises = listOf(
                Exercise.Card("Hi", "haɪ", "chào", "👋", "THÁN TỪ", "Đây là một cách chào hỏi thân mật."),
                Exercise.Choice("Dịch sang tiếng Việt", "hi", listOf("chào", "tôi"), answerIndex = 0, horizontal = true),
                Exercise.TrueFalse(
                    statement = "**Hi** thân mật hơn **hello**.",
                    answer = true,
                    explanation = "**Hi** thường dùng với bạn bè, còn **hello** dùng được trong mọi tình huống.",
                ),
                Exercise.Card("Hello", "həˈləʊ", "xin chào", "😊", "THÁN TỪ", "Dùng được cả trong giao tiếp lịch sự và hằng ngày."),
                Exercise.Choice("Dịch câu này", "Hello, Linh!", listOf("Chào, Linh!", "Tạm biệt, Linh!"), answerIndex = 0),
                Exercise.Tip("I am", "**I am** có nghĩa là ‘tôi là’. Cụm từ này được dùng để giới thiệu bản thân."),
                Exercise.Fill("Tôi là Simon.", "___ ___ Simon.", listOf("I", "am"), listOf("I", "am", "you")),
                Exercise.Tip(
                    "Lưu ý!",
                    "Khác với tiếng Việt, trong tiếng Anh luôn cần có đại từ nhân xưng (như **I**). Bạn không thể nói **am Rafael**, mà phải luôn nói **I am Rafael**.",
                ),
                Exercise.Choice("Dịch câu này", "Tôi là Mai.", listOf("I am Mai.", "Hello, Mai."), answerIndex = 0),
                Exercise.Listen("Nghe và chọn câu \"Xin chào\"", listOf("Hello", "Goodbye", "Thank you"), answerIndex = 0),
                Exercise.Order("Tôi là Minh.", listOf("I", "am", "Minh"), listOf("am", "Minh", "I")),
            ),
        ),
        LocalLessonRecord(
            id = 2,
            chapterTitle = "Chào hỏi và giao tiếp cơ bản",
            title = "Cách xưng hô lịch sự",
            wordCount = 3,
            exercises = listOf(
                Exercise.Card("Good morning", "ɡʊd ˈmɔːnɪŋ", "chào buổi sáng", "🌅", "CỤM TỪ"),
                Exercise.Card("Thank you", "θæŋk juː", "cảm ơn bạn", "🙏", "THÁN TỪ"),
                Exercise.Card("Please", "pliːz", "làm ơn", "✨", "PHÓ TỪ"),
                Exercise.Match(
                    listOf(
                        MatchPair("Good morning", "🌅"),
                        MatchPair("Thank you", "🙏"),
                        MatchPair("Please", "✨"),
                    )
                ),
                Exercise.Fill("Cảm ơn bạn rất nhiều.", "___ ___ very much.", listOf("Thank", "you"), listOf("Thank", "you", "Hi")),
                Exercise.Listen("Nghe và chọn \"Chào buổi sáng\"", listOf("Good night", "Good morning", "Goodbye"), answerIndex = 1),
            ),
        ),
        LocalLessonRecord(
            id = 3,
            chapterTitle = "Ở nhà hàng",
            title = "Các món ăn phổ biến",
            wordCount = 3,
            exercises = listOf(
                Exercise.Card("Water", "ˈwɔːtər", "nước uống", "💧", "DANH TỪ"),
                Exercise.Card("Bread", "bred", "bánh mì", "🍞", "DANH TỪ"),
                Exercise.Card("Coffee", "ˈkɒfi", "cà phê", "☕", "DANH TỪ"),
                Exercise.Match(
                    listOf(
                        MatchPair("Water", "💧"),
                        MatchPair("Bread", "🍞"),
                        MatchPair("Coffee", "☕"),
                    )
                ),
                Exercise.Fill("Làm ơn cho tôi nước uống.", "___, please.", listOf("Water"), listOf("Water", "Bread", "Coffee")),
                Exercise.TrueFalse("**Coffee** có nghĩa là ‘nước uống’.", false, "**Coffee** nghĩa là ‘cà phê’."),
            ),
        ),
        LocalLessonRecord(
            id = 4,
            chapterTitle = "Tham quan thành phố",
            title = "Khám phá xung quanh",
            wordCount = 3,
            exercises = listOf(
                Exercise.Card("Map", "mæp", "bản đồ", "🗺️", "DANH TỪ"),
                Exercise.Card("Street", "striːt", "đường phố", "🛣️", "DANH TỪ"),
                Exercise.Card("Hotel", "həʊˈtel", "khách sạn", "🏨", "DANH TỪ"),
                Exercise.Match(
                    listOf(
                        MatchPair("Map", "🗺️"),
                        MatchPair("Street", "🛣️"),
                        MatchPair("Hotel", "🏨"),
                    )
                ),
                Exercise.Listen("Nghe và chọn từ \"Khách sạn\"", listOf("Map", "Street", "Hotel"), answerIndex = 2),
            ),
        ),
    )

    fun findDetail(id: Int): LessonDetail? =
        lessons.find { it.id == id }?.let { LessonDetail(it.id, it.title, it.exercises) }
}
