from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from . import models  # noqa: F401  (đăng ký bảng)
from .api import router
from .config import settings
from .db import Base, SessionLocal, engine
from .seed import seed


@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(engine)
    if settings.seed_on_startup:
        with SessionLocal() as db:
            seed(db)
    yield


app = FastAPI(title="English Learning API", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
settings.assets_dir.mkdir(parents=True, exist_ok=True)
app.mount("/assets", StaticFiles(directory=settings.assets_dir), name="assets")
app.include_router(router)


@app.get("/health")
def health():
    return {"status": "ok"}
