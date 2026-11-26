# English Cat — Ứng dụng Android (tách rời Backend)

Ứng dụng Android Kotlin + Jetpack Compose được tách kiến trúc theo 3 tầng:

- `domain/`: Mô hình bài học, từ vựng và quy tắc chấm/gợi ý.
- `data/`: Tách riêng nguồn dữ liệu từ xa (`BackendClient`), lưu trữ cục bộ (`SessionStore`, `VocabStore`, `LocalCurriculum`) và `LearningRepository`.
- `ui/`: Giao diện Compose chỉ giao tiếp qua `LearningRepository`, hoàn toàn độc lập với giao thức HTTP hay backend.

## Hai chế độ hoạt động

1. **Học Offline (độc lập 100%):** Không cần đăng nhập hay kết nối máy chủ, dùng bộ bài học tích hợp sẵn (`LocalCurriculum`) và lưu tiến độ XP/chuỗi/từ vựng ngay trên máy.
2. **Kết nối Backend Online:** Đăng nhập/đồng bộ với `english-cat-backend` hoặc bất kỳ máy chủ tương thích qua cấu hình `API_BASE_URL` khi build hoặc chỉnh trực tiếp trên màn hình đăng nhập.

## Build APK

Mở thư mục `android/` trong Android Studio (JDK 17) hoặc chạy:

```bash
gradle :app:assembleDebug
```
