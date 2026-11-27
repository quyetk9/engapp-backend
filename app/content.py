"""Đọc + kiểm tra toàn bộ nội dung trong content/."""
import json
from pathlib import Path

from pydantic import ValidationError

from .step_schemas import validate_step


class ContentError(Exception):
    pass


def _read(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def load_content(root: Path) -> dict:
    errors: list[str] = []
    course = _read(root / "course.json")
    vocab = _read(root / "vocab.json")
    vocab_ids = {v["id"] for v in vocab}
    if len(vocab_ids) != len(vocab):
        errors.append("vocab.json có id bị trùng")

    units, seen_lessons = [], set()
    for udir in sorted(p for p in (root / "units").iterdir() if p.is_dir()):
        unit = _read(udir / "unit.json")
        lessons = []
        for f in sorted((udir / "lessons").glob("*.json")):
            lesson = _read(f)
            lid = lesson["id"]
            if lid in seen_lessons:
                errors.append(f"{f.name}: lesson id '{lid}' bị trùng")
            seen_lessons.add(lid)
            steps = []
            for i, raw in enumerate(lesson.get("steps", []), start=1):
                try:
                    steps.append(validate_step(raw))
                except ValidationError as e:
                    err = e.errors()[0]
                    where = ".".join(str(x) for x in err["loc"])
                    errors.append(f"{f.name} bước {i}: {where} {err['msg']}")
            refs = [s["vocab"] for s in steps if s.get("vocab")] + lesson.get("vocab", [])
            for r in refs:
                if r not in vocab_ids:
                    errors.append(f"{f.name}: vocab '{r}' không có trong vocab.json")
            derived = [s["vocab"] for s in steps if s["type"] == "word_card"]
            lesson_vocab = list(dict.fromkeys(lesson.get("vocab") or derived))
            lessons.append({"id": lid, "title": lesson["title"],
                            "type": lesson.get("type", "lesson"),
                            "vocab_ids": lesson_vocab, "steps": steps})
        units.append({**unit, "lessons": lessons})
    if errors:
        raise ContentError("\n".join(errors))
    return {"course": course, "vocab": vocab, "units": units}
