"""Định nghĩa các loại bước (màn hình) trong một bài học.

Mỗi step trong file JSON phải khớp một trong các lớp dưới đây (phân biệt bằng "type").
Thêm loại bài mới = thêm một lớp ở đây, không phải đổi bảng DB.
"""
from collections import Counter
from typing import Annotated, Literal, Union

from pydantic import BaseModel, ConfigDict, Field, TypeAdapter, model_validator


class _Base(BaseModel):
    model_config = ConfigDict(extra="forbid")
    hint: str | None = None      # nút bóng đèn
    skippable: bool = True       # nút »


def _check_choice(options: list[str], answer: int):
    if len(set(options)) != len(options):
        raise ValueError("options bị trùng")
    if not 0 <= answer < len(options):
        raise ValueError("answer nằm ngoài options")


class LessonIntro(_Base):
    type: Literal["lesson_intro"]
    text_vi: str


class WordCard(_Base):
    type: Literal["word_card"]
    vocab: str
    note_vi: str | None = None
    image: str | None = None


class Explain(_Base):
    type: Literal["explain"]
    text_vi: str
    image: str | None = None


class PronunciationTip(_Base):
    type: Literal["pronunciation_tip"]
    text_vi: str
    ipa: str | None = None


class GrammarNote(_Base):
    type: Literal["grammar_note"]
    title: str = "Lưu ý!"
    text_vi: str
    highlight: list[str] = []


class McqText(_Base):
    type: Literal["mcq_text"]
    prompt: str = "Dịch sang tiếng Việt"
    question: str
    options: list[str] = Field(min_length=2)
    answer: int

    @model_validator(mode="after")
    def _v(self):
        _check_choice(self.options, self.answer)
        return self


class TrueFalse(_Base):
    type: Literal["true_false"]
    prompt: str = "Đúng hay sai"
    statement: str
    answer: bool


class WordOrder(_Base):
    type: Literal["word_order"]
    prompt: str = "Dịch câu này"
    source: str                  # câu gốc cần dịch
    tiles: list[str]             # các ô từ (đã gồm từ gây nhiễu)
    answer: list[str]            # thứ tự đúng
    suffix: str | None = None    # phần câu cho sẵn, ví dụ "Simon."

    @model_validator(mode="after")
    def _v(self):
        if Counter(self.answer) - Counter(self.tiles):
            raise ValueError("answer chứa từ không có trong tiles")
        return self


class MatchPairs(_Base):
    type: Literal["match_pairs"]
    prompt: str = "Ghép các cặp sau"
    pairs: list[tuple[str, str]] = Field(min_length=2)


class ImageMcq(_Base):
    type: Literal["image_mcq"]
    prompt: str = "Trong hình có gì?"
    image: str
    options: list[str] = Field(min_length=2)
    answer: int

    @model_validator(mode="after")
    def _v(self):
        _check_choice(self.options, self.answer)
        return self


class FillBlank(_Base):
    type: Literal["fill_blank"]
    prompt: str = "Điền vào chỗ trống"
    sentence: str                # chứa "___"
    image: str | None = None
    tiles: list[str] = Field(min_length=2)
    answer: str

    @model_validator(mode="after")
    def _v(self):
        if "___" not in self.sentence:
            raise ValueError('sentence phải chứa "___"')
        if self.answer not in self.tiles:
            raise ValueError("answer không nằm trong tiles")
        return self


class ListeningMcq(_Base):
    type: Literal["listening_mcq"]
    prompt: str = "Nghe và chọn nghĩa đúng"
    audio_text: str              # câu tiếng Anh cần phát (audio file hoặc TTS trên máy)
    vocab: str | None = None
    options: list[str] = Field(min_length=2)
    answer: int

    @model_validator(mode="after")
    def _v(self):
        _check_choice(self.options, self.answer)
        return self


class Speaking(_Base):
    type: Literal["speaking"]
    prompt: str = "Chạm để nói"
    source: str                  # câu tiếng Việt hiển thị
    target: str                  # câu tiếng Anh cần nói


Step = Annotated[
    Union[LessonIntro, WordCard, Explain, PronunciationTip, GrammarNote, McqText,
          TrueFalse, WordOrder, MatchPairs, ImageMcq, FillBlank, ListeningMcq, Speaking],
    Field(discriminator="type"),
]
_ADAPTER = TypeAdapter(Step)


def validate_step(data: dict) -> dict:
    return _ADAPTER.validate_python(data).model_dump(mode="json")
