# English Learning API

Backend cho app học tiếng Anh (người Việt): lộ trình unit → bài học → các bước, tiến độ, XP, streak,
ôn từ vựng ngắt quãng, giải đấu tuần. Không gồm tính năng AI hội thoại.

## Chạy thử trên máy
```bash
pip install -r requirements-dev.txt
python -m app.validate      # kiểm tra nội dung
pytest -q
uvicorn app.main:app --reload   # http://localhost:8000/docs
```

## Deploy lên Render
1. Đẩy repo lên GitHub.
2. Render → **New → Blueprint** → chọn repo (đọc `render.yaml`, tạo web service + Postgres).
3. Đặt biến `ADMIN_TOKEN` trong Dashboard. Từ đó mỗi lần `git push` Render tự build và deploy.
4. (Tuỳ chọn) thêm secrets `API_URL`, `ADMIN_TOKEN` vào GitHub để workflow `weekly-league.yml` chốt giải đấu mỗi thứ Hai.

Lưu ý gói free: web service ngủ khi không có truy cập (request đầu chậm), Postgres free có hạn dùng
giới hạn nên cần nâng cấp hoặc sao lưu trước khi hết hạn.

## Thêm nội dung (không cần sửa code)
```
content/
  course.json
  vocab.json                      # từ vựng dùng chung
  units/01-greetings/unit.json
  units/01-greetings/lessons/01-hi-hello.json   # tên file quyết định thứ tự
```
Một lesson là danh sách `steps`, mỗi step có `type` (xem `app/step_schemas.py`):
`lesson_intro, word_card, explain, pronunciation_tip, grammar_note, mcq_text, true_false,
word_order, match_pairs, image_mcq, fill_blank, listening_mcq, speaking`.
`"type": "vocab_practice"` ở cấp lesson = bài luyện từ vựng tự sinh từ các từ người dùng đã học.
Ảnh/audio: bỏ file vào `assets/images/<vocab_id>.png|webp|jpg` và `assets/audio/<vocab_id>.mp3`,
server tự gắn URL khi khởi động. CI sẽ báo lỗi nếu step sai định dạng.

## API
| Method | Đường dẫn | Mô tả |
|---|---|---|
| POST | `/users` | tạo người dùng ẩn danh, trả `token` (chỉ hiện một lần) |
| GET | `/me` | xp, streak, coins, hạng giải |
| GET | `/course` | unit + lesson + trạng thái `completed/current/locked` |
| GET | `/lessons/{id}` | danh sách step (đã kèm `vocab_data`) |
| POST | `/lessons/{id}/complete` | `{correct,total,seconds,vocab_results?}` → xp, accuracy, new_vocab, streak, league_rank |
| GET | `/vocab/due` | từ đến hạn ôn |
| GET | `/league` | bảng xếp hạng tuần (zone `promote`/`demote`) |
| POST | `/admin/league/rollover` | chốt tuần trước (header `X-Admin-Token`) |

Gửi token bằng header `Authorization: Bearer <token>`.
Các ảnh/audio phục vụ tại `/assets/...`.
