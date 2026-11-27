import hashlib
import hmac
import secrets
import uuid
from datetime import timedelta

from fastapi import APIRouter, Depends, Header, HTTPException
from pydantic import BaseModel, Field, model_validator
from sqlalchemy import select
from sqlalchemy.orm import Session

from . import services
from .config import settings
from .db import get_db
from .models import (Course, Lesson, LessonResult, Step, Unit, User, UserProgress, UserVocab, Vocab)

router = APIRouter()


# ---------- xác thực: token ngẫu nhiên cấp một lần, DB chỉ lưu hash ----------
def _hash(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


def optional_user(db: Session = Depends(get_db), authorization: str | None = Header(None)) -> User | None:
    if not authorization or not authorization.lower().startswith("bearer "):
        return None
    return db.scalar(select(User).where(User.token_hash == _hash(authorization[7:].strip())))


def current_user(user: User | None = Depends(optional_user)) -> User:
    if user is None:
        raise HTTPException(401, "Thiếu hoặc sai token")
    return user


class NewUser(BaseModel):
    display_name: str | None = Field(default=None, max_length=50)


@router.post("/users", status_code=201)
def create_user(body: NewUser, db: Session = Depends(get_db)):
    token = secrets.token_urlsafe(32)
    user = User(id=str(uuid.uuid4()), token_hash=_hash(token), display_name=body.display_name)
    db.add(user)
    db.commit()
    return {"id": user.id, "token": token}   # token chỉ trả về lần này


def _profile(u: User) -> dict:
    return {"id": u.id, "display_name": u.display_name, "xp": u.xp, "streak": u.streak,
            "coins": u.coins, "league_tier": u.league_tier}


@router.get("/me")
def me(user: User = Depends(current_user)):
    return _profile(user)


# ---------- lộ trình học ----------
@router.get("/course")
def get_course(db: Session = Depends(get_db), user: User | None = Depends(optional_user)):
    course = db.scalars(select(Course)).first()
    if course is None:
        raise HTTPException(404, "Chưa có khoá học")
    done = set()
    if user:
        done = set(db.scalars(select(UserProgress.lesson_id).where(UserProgress.user_id == user.id)))
    units = db.scalars(select(Unit).where(Unit.course_id == course.id).order_by(Unit.position)).all()
    current_found, out_units = False, []
    for unit in units:
        lessons = db.scalars(select(Lesson).where(Lesson.unit_id == unit.id)
                             .order_by(Lesson.position)).all()
        words = {w for lsn in lessons for w in lsn.vocab_ids}
        out_lessons = []
        for lsn in lessons:
            if lsn.id in done:
                status = "completed"
            elif not current_found:
                status, current_found = "current", True
            else:
                status = "locked"
            out_lessons.append({"id": lsn.id, "title": lsn.title, "type": lsn.type, "status": status})
        out_units.append({"id": unit.id, "title": unit.title, "description": unit.description,
                          "banner_image": unit.banner_image, "topic_count": len(lessons),
                          "word_count": len(words), "lessons": out_lessons})
    return {"id": course.id, "title": course.title, "units": out_units,
            "profile": _profile(user) if user else None}


def _expand(db: Session, steps: list[Step]) -> list[dict]:
    ids = {s.payload["vocab"] for s in steps if s.payload.get("vocab")}
    vmap = {v.id: v for v in db.scalars(select(Vocab).where(Vocab.id.in_(ids)))} if ids else {}
    out = []
    for i, s in enumerate(steps):
        p = dict(s.payload)
        v = vmap.get(p.get("vocab"))
        if v:
            p["vocab_data"] = {"id": v.id, "word": v.word, "ipa": v.ipa, "meaning_vi": v.meaning_vi,
                               "example_en": v.example_en, "example_vi": v.example_vi,
                               "audio_url": v.audio_url, "image_url": v.image_url}
        out.append({"index": i, **p})
    return out


@router.get("/lessons/{lesson_id}")
def get_lesson(lesson_id: str, db: Session = Depends(get_db), user: User | None = Depends(optional_user)):
    lesson = db.get(Lesson, lesson_id)
    if lesson is None:
        raise HTTPException(404, "Không có bài học này")
    if lesson.type == "vocab_practice":
        if user is None:
            raise HTTPException(401, "Cần đăng nhập để luyện từ vựng")
        generated = services.vocab_practice_steps(db, user)
        if not generated:
            raise HTTPException(409, "Chưa có từ vựng nào để luyện, hãy học bài trước")
        steps = [{"index": i, **s} for i, s in enumerate(generated)]
    else:
        rows = db.scalars(select(Step).where(Step.lesson_id == lesson_id).order_by(Step.position)).all()
        steps = _expand(db, rows)
    return {"id": lesson.id, "title": lesson.title, "type": lesson.type,
            "unit_id": lesson.unit_id, "steps": steps}


class VocabResult(BaseModel):
    vocab_id: str
    correct: bool


class CompleteIn(BaseModel):
    correct: int = Field(ge=0)
    total: int = Field(ge=0, le=500)
    seconds: int = Field(ge=0, le=86400)
    vocab_results: list[VocabResult] | None = None   # bài luyện từ vựng gửi kết quả từng từ

    @model_validator(mode="after")
    def _v(self):
        if self.correct > self.total:
            raise ValueError("correct không được lớn hơn total")
        return self


@router.post("/lessons/{lesson_id}/complete")
def complete_lesson(lesson_id: str, body: CompleteIn, db: Session = Depends(get_db),
                    user: User = Depends(current_user)):
    lesson = db.get(Lesson, lesson_id)
    if lesson is None:
        raise HTTPException(404, "Không có bài học này")
    d = services.today()
    accuracy = body.correct / body.total if body.total else 1.0
    progress = db.get(UserProgress, (user.id, lesson.id))
    xp = services.calc_xp(accuracy, replay=progress is not None)

    pairs = ([(r.vocab_id, r.correct) for r in body.vocab_results] if body.vocab_results
             else [(v, accuracy >= 0.7) for v in lesson.vocab_ids])
    new_vocab = services.update_vocab(db, user, pairs, d)

    if progress is None:
        db.add(UserProgress(user_id=user.id, lesson_id=lesson.id, best_accuracy=accuracy))
        user.coins += 5
    else:
        progress.completions += 1
        progress.best_accuracy = max(progress.best_accuracy, accuracy)
    user.xp += xp
    services.update_streak(user, d)
    services.ensure_member(db, user, d).weekly_xp += xp
    db.add(LessonResult(user_id=user.id, lesson_id=lesson.id, xp=xp, seconds=body.seconds,
                        accuracy=accuracy, new_vocab=new_vocab))
    db.flush()
    rank = services.standings(db, user, d)["my_rank"]
    db.commit()
    return {"xp": xp, "seconds": body.seconds, "accuracy": round(accuracy, 3), "new_vocab": new_vocab,
            "streak": user.streak, "coins": user.coins, "total_xp": user.xp, "league_rank": rank}


@router.get("/vocab/due")
def vocab_due(db: Session = Depends(get_db), user: User = Depends(current_user)):
    rows = db.execute(
        select(UserVocab, Vocab).join(Vocab, Vocab.id == UserVocab.vocab_id)
        .where(UserVocab.user_id == user.id, UserVocab.next_review <= services.today())
        .order_by(UserVocab.next_review)
    ).all()
    return {"count": len(rows), "items": [
        {"id": v.id, "word": v.word, "meaning_vi": v.meaning_vi, "strength": uv.strength}
        for uv, v in rows]}


@router.get("/league")
def league(db: Session = Depends(get_db), user: User = Depends(current_user)):
    data = services.standings(db, user, services.today())
    db.commit()
    return data


@router.post("/admin/league/rollover")
def admin_rollover(db: Session = Depends(get_db), x_admin_token: str | None = Header(None)):
    if not settings.admin_token or not hmac.compare_digest(x_admin_token or "", settings.admin_token):
        raise HTTPException(403, "Không có quyền")
    last_week = services.week_start(services.today()) - timedelta(days=7)
    return {"week_start": last_week.isoformat(), "changed": services.rollover(db, last_week)}
