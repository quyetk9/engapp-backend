"""Nạp nội dung từ content/ vào DB. Chạy lại bao nhiêu lần cũng được, không đụng dữ liệu người dùng."""
from pathlib import Path

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from .config import settings
from .content import load_content
from .models import (Course, Lesson, LessonResult, Step, Unit, UserProgress, Vocab)


def _find_asset(folder: Path, stem: str, exts: tuple[str, ...]) -> str | None:
    for ext in exts:
        if (folder / f"{stem}{ext}").exists():
            return f"/assets/{folder.name}/{stem}{ext}"
    return None


def seed(db: Session, content_dir: Path | None = None, assets_dir: Path | None = None) -> None:
    data = load_content(content_dir or settings.content_dir)
    assets = assets_dir or settings.assets_dir
    course = data["course"]
    db.merge(Course(**course))

    for v in data["vocab"]:
        db.merge(Vocab(
            **v,
            audio_url=_find_asset(assets / "audio", v["id"], (".mp3", ".ogg")),
            image_url=_find_asset(assets / "images", v["id"], (".webp", ".png", ".jpg")),
        ))

    unit_ids, lesson_ids = [], []
    for ui, unit in enumerate(data["units"]):
        unit_ids.append(unit["id"])
        db.merge(Unit(id=unit["id"], course_id=course["id"], position=ui, title=unit["title"],
                      description=unit.get("description"), banner_image=unit.get("banner_image")))
        for li, lesson in enumerate(unit["lessons"]):
            lesson_ids.append(lesson["id"])
            db.merge(Lesson(id=lesson["id"], unit_id=unit["id"], position=li, title=lesson["title"],
                            type=lesson["type"], vocab_ids=lesson["vocab_ids"]))
            db.flush()
            db.execute(delete(Step).where(Step.lesson_id == lesson["id"]))
            for si, s in enumerate(lesson["steps"]):
                db.add(Step(lesson_id=lesson["id"], position=si, type=s["type"], payload=s))

    # dọn bài/unit đã bị xoá khỏi content/
    stale = list(db.scalars(select(Lesson.id).where(Lesson.id.not_in(lesson_ids))))
    if stale:
        for model in (Step, UserProgress, LessonResult):
            db.execute(delete(model).where(model.lesson_id.in_(stale)))
        db.execute(delete(Lesson).where(Lesson.id.in_(stale)))
    db.execute(delete(Unit).where(Unit.id.not_in(unit_ids)))
    db.commit()
