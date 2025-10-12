from __future__ import annotations
from typing import List
from datetime import datetime
from sqlalchemy import String, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database.base import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    login_id: Mapped[str] = mapped_column(String(50), unique=True, nullable=False)
    hashed_password: Mapped[str] = mapped_column(String(100), nullable=False)
    username: Mapped[str] = mapped_column(String(100), nullable=True)

    journals: Mapped[List["Journal"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    emotions: Mapped[List["Emotion"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    emotion_events: Mapped[List["EmotionEvent"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    keywords: Mapped[List["Keyword"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    questions: Mapped[List["Question"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    answers: Mapped[List["Answer"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    value_scores: Mapped[List["ValueScore"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    value_maps: Mapped[List["ValueMap"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    recommendations: Mapped[List["Recommendation"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    conversations: Mapped[List["Conversation"]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
