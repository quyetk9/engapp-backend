# English Cat — backend riêng

Backend Node.js + SQLite, được đóng gói tách khỏi ứng dụng Android. Gói này không tự tạo máy chủ công khai; bạn cần chạy nó trên máy tính/VPS hoặc dịch vụ máy chủ của mình rồi build ứng dụng trỏ tới URL đó.

## Chạy thử trên máy

Yêu cầu Node.js 22.5 trở lên. Backend không cần cài thư viện ngoài.

```bash
npm test
mkdir -p data
export JWT_SECRET="$(openssl rand -hex 32)"
export DB_FILE="$PWD/data/engapp.db"
export PORT=3000
npm start
```

Kiểm tra trạng thái tại `http://localhost:3000/health`. Dữ liệu SQLite được lưu trong file do `DB_FILE` chỉ định.

## Chạy bằng Docker Compose

Cài Docker và Docker Compose, tạo secret riêng rồi chạy:

```bash
cp .env.example .env
openssl rand -hex 32
```

Dán chuỗi ngẫu nhiên vừa tạo vào giá trị `JWT_SECRET` trong `.env`, sau đó:

```bash
docker compose up -d --build
docker compose logs -f
```

SQLite được giữ trong volume Docker `engapp-data` khi container được cập nhật. Backend nghe cổng 3000; hãy cấu hình firewall/reverse proxy phù hợp nếu truy cập từ Internet. Trên máy chủ công khai, đặt HTTPS qua reverse proxy (ví dụ Caddy/Nginx), không mở giao thức HTTP thô cho người dùng, giữ secret riêng tư và sao lưu volume dữ liệu định kỳ.

## Trỏ ứng dụng Android tới backend

Sau khi backend có URL truy cập được, build APK Android với dấu `/` cuối URL:

```bash
gradle :app:assembleDebug -PAPI_BASE_URL=https://api.ten-mien-cua-ban/
```

Nếu chỉ thử trong cùng Wi-Fi, có thể dùng IP LAN của máy chạy backend, ví dụ `http://192.168.1.10:3000/`. Không dùng `10.0.2.2` trên điện thoại thật; địa chỉ đó chỉ dành cho Android Emulator. Gói ứng dụng Android và gói backend được bàn giao riêng.

## API

`POST /api/auth/register`, `POST /api/auth/login`, `GET /api/me`, `GET /api/lessons`, `GET /api/lessons/:id`, `POST /api/lessons/:id/complete`, `GET /api/words`, `GET /api/leaderboard`; endpoint kiểm tra `GET /health`. Các route dữ liệu cần `Authorization: Bearer <token>`.

Bản backend này dành cho phát triển/thử nghiệm. Trước khi phục vụ rộng rãi nên đánh giá thêm giới hạn tốc độ đăng nhập, giám sát, backup và cấu hình HTTPS.
