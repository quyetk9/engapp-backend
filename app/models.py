from datetime import date, datetime, timezone

from sqlalchemy import JSON, Date, DateTime, Float, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from .db import Base


def _now() -> datetime:
    return datetime.now(timezone.utc)


# ---------- NỘI DUNG (nạp từ content/, không chứa dữ liệu người dùng) ----------
class Course(Base):
    __tablename__ = "courses"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    title: Mapped[str] = mapped_column(String(200))
    from_lang: Mapped[str] = mapped_column(String(8))
    to_lang: Mapped[str] = mapped_column(String(8))


class Unit(Base):
    __tablename__ = "units"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    course_id: Mapped[str] = mapped_column(ForeignKey("courses.id"), index=True)
    position: Mapped[int] = mapped_column(Integer)
    title: Mapped[str] = mapped_column(String(200))
    description: Mapped[str | None] = mapped_column(String(500), nullable=True)
    banner_image: Mapped[str | None] = mapped_column(String(300), nullable=True)


class Lesson(Base):
    __tablename__ = "lessons"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    unit_id: Mapped[str] = mapped_column(ForeignKey("units.id"), index=True)
    position: Mapped[int] = mapped_column(Integer)
    title: Mapped[str] = mapped_column(String(200))
    type: Mapped[str] = mapped_column(String(32), default="lesson")  # lesson | vocab_practice
    vocab_ids: Mapped[list] = mapped_column(JSON, default=list)


class Step(Base):
    __tablename__ = "steps"
    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    lesson_id: Mapped[str] = mapped_column(ForeignKey("lessons.id"), index=True)
    position: Mapped[int] = mapped_column(Integer)
    type: Mapped[str] = mapped_column(String(32))
    payload: Mapped[dict] = mapped_column(JSON)


class Vocab(Base):
    __tablename__ = "vocab"
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    word: Mapped[str] = mapped_column(String(100))
    ipa: Mapped[str | None] = mapped_column(String(100), nullable=True)
    meaning_vi: Mapped[str] = mapped_column(String(200))
    example_en: Mapped[str | None] = mapped_column(String(300), nullable=True)
    example_vi: Mapped[str | None] = mapped_column(String(300), nullable=True)
    audio_url: Mapped[str | None] = mapped_column(String(300), nullable=True)
    image_url: Mapped[str | None] = mapped_column(String(300), nullable=True)


# ---------- NGƯỜI DÙNG & TIẾN ĐỘ ----------
class User(Base):
    __tablename__ = "users"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    token_hash: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    display_name: Mapped[str | None] = mapped_column(String(50), nullable=True)
    xp: Mapped[int] = mapped_column(Integer, default=0)
    streak: Mapped[int] = mapped_column(Integer, default=0)
    coins: Mapped[int] = mapped_column(Integer, default=0)
    league_tier: Mapped[int] = mapped_column(Integer, default=1)
    last_active_date: Mapped[date | None] = mapped_column(Date, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_now)


class UserProgress(Base):
    __tablename__ = "user_progress"
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), primary_key=True)
    lesson_id: Mapped[str] = mapped_column(ForeignKey("lessons.id"), primary_key=True)
    completions: Mapped[int] = mapped_column(Integer, default=1)
    best_accuracy: Mapped[float] = mapped_column(Float, default=0.0)
    completed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_now)


class UserVocab(Base):
    __tablename__ = "user_vocab"
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), primary_key=True)
    vocab_id: Mapped[str] = mapped_column(ForeignKey("vocab.id"), primary_key=True)
    strength: Mapped[int] = mapped_column(Integer, default=0)  # 0..6
    next_review: Mapped[date] = mapped_column(Date)


class LessonResult(Base):
    __tablename__ = "lesson_results"
    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), index=True)
    lesson_id: Mapped[str] = mapped_column(ForeignKey("lessons.id"))
    xp: Mapped[int] = mapped_column(Integer)
    seconds: Mapped[int] = mapped_column(Integer)
    accuracy: Mapped[float] = mapped_column(Float)
    new_vocab: Mapped[int] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_now)


# ---------- GIẢI ĐẤU TUẦN ----------
class LeagueMember(Base):
    __tablename__ = "league_members"
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id"), primary_key=True)
    week_start: Mapped[date] = mapped_column(Date, primary_key=True)
    tier: Mapped[int] = mapped_column(Integer, index=True)
    group_no: Mapped[int] = mapped_column(Integer, index=True)
    weekly_xp: Mapped[int] = mapped_column(Integer, default=0)


class LeagueRollover(Base):
    """Đánh dấu tuần đã chốt thăng/xuống hạng, tránh chạy hai lần."""
    __tablename__ = "league_rollovers"
    week_start: Mapped[date] = mapped_column(Date, primary_key=True)
