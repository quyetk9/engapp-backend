from datetime import date, timedelta

from app import services
from app.db import SessionLocal
from app.models import LeagueMember, User


def test_course_without_login_first_lesson_current(client):
    c = client.get("/course").json()
    assert c["units"][0]["lessons"][0]["status"] == "current"
    assert c["units"][0]["lessons"][1]["status"] == "locked"
    assert c["units"][0]["word_count"] == 5 or c["units"][0]["word_count"] >= 3


def test_full_learning_flow(client, auth):
    lesson = client.get("/lessons/greetings-1", headers=auth).json()
    assert len(lesson["steps"]) == 11
    card = next(s for s in lesson["steps"] if s["type"] == "word_card")
    assert card["vocab_data"]["ipa"] and card["vocab_data"]["meaning_vi"]

    r = client.post("/lessons/greetings-1/complete", headers=auth,
                    json={"correct": 9, "total": 10, "seconds": 124}).json()
    assert r["xp"] == 19 and r["new_vocab"] == 3 and r["streak"] == 1
    assert r["coins"] == 5 and r["league_rank"] == 1

    c = client.get("/course", headers=auth).json()
    assert c["units"][0]["lessons"][0]["status"] == "completed"
    assert c["units"][0]["lessons"][1]["status"] == "current"

    again = client.post("/lessons/greetings-1/complete", headers=auth,
                        json={"correct": 10, "total": 10, "seconds": 90}).json()
    assert again["xp"] == 10 and again["new_vocab"] == 0 and again["coins"] == 5

    assert client.get("/me", headers=auth).json()["xp"] == 29
    lg = client.get("/league", headers=auth).json()
    assert lg["members"][0]["is_me"] and lg["members"][0]["weekly_xp"] == 29


def test_vocab_practice_generated(client, auth):
    assert client.get("/lessons/family-3", headers=auth).status_code == 409
    client.post("/lessons/greetings-1/complete", headers=auth,
                json={"correct": 10, "total": 10, "seconds": 60})
    steps = client.get("/lessons/family-3", headers=auth).json()["steps"]
    assert len(steps) == 3
    for s in steps:
        assert s["options"][s["answer"]]
        assert s["vocab_id"]
    # học đúng hết → hẹn ôn sau 1 ngày, chưa đến hạn
    assert client.get("/vocab/due", headers=auth).json()["count"] == 0
    # luyện từ vựng, trả lời sai 1 từ → độ thuộc giảm, từ đó đến hạn ôn ngay
    r = client.post("/lessons/family-3/complete", headers=auth, json={
        "correct": 2, "total": 3, "seconds": 30,
        "vocab_results": [{"vocab_id": "hi", "correct": False},
                          {"vocab_id": "hello", "correct": True},
                          {"vocab_id": "goodbye", "correct": True}]}).json()
    assert r["new_vocab"] == 0
    due = client.get("/vocab/due", headers=auth).json()
    assert due["count"] == 1 and due["items"][0]["id"] == "hi"
    assert client.get("/lessons/family-3").status_code == 401


def test_validation_and_auth(client, auth):
    assert client.get("/me").status_code == 401
    bad = client.post("/lessons/greetings-1/complete", headers=auth,
                      json={"correct": 11, "total": 10, "seconds": 5})
    assert bad.status_code == 422
    assert client.get("/lessons/nope").status_code == 404
    assert client.post("/admin/league/rollover").status_code == 403


def test_streak_logic():
    u = User(id="x", token_hash="h", streak=0, last_active_date=None)
    d = date(2026, 11, 27)
    services.update_streak(u, d); assert u.streak == 1
    services.update_streak(u, d); assert u.streak == 1                      # cùng ngày
    services.update_streak(u, d + timedelta(days=1)); assert u.streak == 2  # liên tiếp
    services.update_streak(u, d + timedelta(days=4)); assert u.streak == 1  # đứt chuỗi


def test_league_rollover(client):
    ws = services.week_start(services.today()) - timedelta(days=7)
    with SessionLocal() as db:
        for i in range(12):
            db.add(User(id=f"u{i:02d}", token_hash=f"h{i}", league_tier=2))
            db.add(LeagueMember(user_id=f"u{i:02d}", week_start=ws, tier=2, group_no=1,
                                weekly_xp=1000 - i * 10))
        db.commit()
        assert services.rollover(db, ws) == 11          # 5 lên + 6 xuống
        assert db.get(User, "u00").league_tier == 3
        assert db.get(User, "u05").league_tier == 2
        assert db.get(User, "u11").league_tier == 1
        assert services.rollover(db, ws) == 0           # chạy lần hai không đổi gì


def test_admin_rollover_endpoint(client):
    r = client.post("/admin/league/rollover", headers={"X-Admin-Token": "test-admin"})
    assert r.status_code == 200 and "week_start" in r.json()
