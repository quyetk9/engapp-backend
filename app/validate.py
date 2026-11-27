"""python -m app.validate  → kiểm tra nội dung, dùng trong CI."""
import sys

from .config import settings
from .content import ContentError, load_content

try:
    c = load_content(settings.content_dir)
except ContentError as e:
    print("LỖI NỘI DUNG:\n" + str(e))
    sys.exit(1)
n_lessons = sum(len(u["lessons"]) for u in c["units"])
n_steps = sum(len(lsn["steps"]) for u in c["units"] for lsn in u["lessons"])
print(f"OK: {len(c['units'])} unit, {n_lessons} lesson, {n_steps} step, {len(c['vocab'])} từ vựng")
