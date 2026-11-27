"""Tạo file audio phát âm cho từng từ trong content/vocab.json (tuỳ chọn).

    pip install edge-tts
    python -m scripts.generate_audio

Lưu ý: script này chưa được chạy thử trong môi trường dựng repo (cần mạng).
Nếu không tạo audio, app khách có thể dùng TTS của điện thoại với trường `word`.
"""
import asyncio
import json
from pathlib import Path

import edge_tts

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "assets" / "audio"
VOICE = "en-US-JennyNeural"


async def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for v in json.loads((ROOT / "content" / "vocab.json").read_text(encoding="utf-8")):
        target = OUT / f"{v['id']}.mp3"
        if target.exists():
            continue
        await edge_tts.Communicate(v["word"], VOICE).save(str(target))
        print("tạo", target.name)


asyncio.run(main())
