import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def _normalize(url: str) -> str:
    # Render trả về postgres:// hoặc postgresql://, SQLAlchemy + psycopg3 cần postgresql+psycopg://
    if url.startswith("postgres://"):
        url = "postgresql://" + url[len("postgres://"):]
    if url.startswith("postgresql://"):
        url = "postgresql+psycopg://" + url[len("postgresql://"):]
    return url


class Settings:
    database_url = _normalize(os.getenv("DATABASE_URL", "sqlite:///./dev.db"))
    seed_on_startup = os.getenv("SEED_ON_STARTUP", "1") == "1"
    content_dir = Path(os.getenv("CONTENT_DIR", ROOT / "content"))
    assets_dir = Path(os.getenv("ASSETS_DIR", ROOT / "assets"))
    admin_token = os.getenv("ADMIN_TOKEN", "")


settings = Settings()
