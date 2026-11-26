# English Cat — Backend API

Backend độc lập chạy bằng **Node.js (>= 18)** không cần cài đặt gói ngoài (`zero-dependency`), phục vụ dữ liệu bài học, xác thực tài khoản và lưu tiến độ học tập cho ứng dụng Android English Cat.

## Chạy cục bộ

```bash
npm start
```

Mặc định server lắng nghe tại `http://localhost:3000`.

### Biến môi trường

- `PORT`: Cổng HTTP (mặc định `3000`).
- `JWT_SECRET`: Khóa bí mật ký token (nên đặt riêng khi deploy production).
- `DATA_FILE`: Đường dẫn file JSON lưu dữ liệu người dùng (mặc định `./data/db.json`).

## Kiểm thử tự động

```bash
npm test
```

## Các endpoint chính

| Method | Path | Mô tả | Auth |
| --- | --- | --- | --- |
| GET | `/health` | Kiểm tra trạng thái server (`{"ok":true}`) | Không |
| POST | `/api/auth/register` | Đăng ký tài khoản (`name`, `email`, `password`) | Không |
| POST | `/api/auth/login` | Đăng nhập (`email`, `password`) | Không |
| GET | `/api/me` | Lấy thông tin người dùng (`xp`, `streak`) | Bearer Token |
| GET | `/api/lessons` | Danh sách chương/bài học kèm tiến độ | Bearer Token |
| GET | `/api/lessons/:id` | Chi tiết các thẻ và bài tập của bài học | Bearer Token |
| POST | `/api/lessons/:id/complete` | Nộp kết quả (`correct`, `total`, `seconds`) | Bearer Token |
