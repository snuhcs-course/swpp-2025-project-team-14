from __future__ import annotations

from backend.app.database.models.emotion_event_model import (
    Answer,
    Conversation,
    Emotion,
    EmotionEvent,
    Journal,
    Keyword,
    Question,
    Recommendation,
    ValueMap,
    ValueScore,
)
from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    login_id: Mapped[str] = mapped_column(String(50), unique=True, nullable=False)
    hashed_password: Mapped[str] = mapped_column(String(100), nullable=False)
    username: Mapped[str] = mapped_column(String(100), nullable=True)

    journals: Mapped[list[Journal]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    emotions: Mapped[list[Emotion]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    emotion_events: Mapped[list[EmotionEvent]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    keywords: Mapped[list[Keyword]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    questions: Mapped[list[Question]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    answers: Mapped[list[Answer]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    value_scores: Mapped[list[ValueScore]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    value_maps: Mapped[list[ValueMap]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    recommendations: Mapped[list[Recommendation]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    conversations: Mapped[list[Conversation]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
