import pytest
from pydantic import ValidationError

from app.config import settings
from app.content import load_content
from app.step_schemas import validate_step


def test_sample_content_is_valid():
    c = load_content(settings.content_dir)
    assert len(c["units"]) >= 2 and len(c["vocab"]) >= 9


@pytest.mark.parametrize("bad", [
    {"type": "mcq_text", "question": "x", "options": ["a", "b"], "answer": 2},
    {"type": "mcq_text", "question": "x", "options": ["a", "a"], "answer": 0},
    {"type": "word_order", "source": "s", "tiles": ["a"], "answer": ["a", "b"]},
    {"type": "fill_blank", "sentence": "no blank", "tiles": ["a", "b"], "answer": "a"},
    {"type": "fill_blank", "sentence": "x ___", "tiles": ["a", "b"], "answer": "c"},
    {"type": "unknown_type"},
    {"type": "true_false", "statement": "s", "answer": True, "bogus": 1},
])
def test_bad_steps_rejected(bad):
    with pytest.raises(ValidationError):
        validate_step(bad)


def test_image_mcq_ok():
    s = validate_step({"type": "image_mcq", "image": "/assets/images/mother.png",
                       "options": ["father", "mother"], "answer": 1})
    assert s["prompt"] == "Trong hình có gì?"
