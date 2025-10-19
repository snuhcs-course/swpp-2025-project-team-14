from __future__ import annotations
from datetime import datetime
from typing import List, Optional
from sqlalchemy import String, Integer, Text, ForeignKey, DateTime, JSON, Float
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database.base import Base
from app.features.user.models import User

# Journal class는 테스트 용 merge 후 삭제 예정
class Journal(Base):
    __tablename__ = "journals"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)

    title: Mapped[str] = mapped_column(String(255), nullable=False)
    keywords: Mapped[Optional[str]] = mapped_column(Text, nullable=True)  # JSON 문자열로 저장
    content: Mapped[str] = mapped_column(Text, nullable=False)
    gratitude: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="journals")  # N:1 (User → Journal)

class Question(Base):
    __tablename__ = "questions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    type: Mapped[str | None] = mapped_column(String(50), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="questions")
    answers: Mapped["Answer"] = relationship(back_populates="question", uselist=False, cascade="all, delete")

class Answer(Base):
    __tablename__ = "answers"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    question_id: Mapped[int] = mapped_column(ForeignKey("questions.id", ondelete="CASCADE"), nullable=False)
    type: Mapped[str | None] = mapped_column(String(50), nullable=True)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    keywords: Mapped[str | None] = mapped_column(Text, nullable=True)  # JSON 문자열
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="answers")
    question: Mapped["Question"] = relationship(back_populates="answers")

class ValueMap(Base):
    __tablename__ = "value_maps"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    value_map: Mapped[dict] = mapped_column(JSON, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="value_maps")

class ValueScore(Base):
    __tablename__ = "value_scores"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    question_id: Mapped[int] = mapped_column(ForeignKey("questions.id", ondelete="SET NULL"), nullable=True)
    answer_id: Mapped[int] = mapped_column(ForeignKey("answers.id", ondelete="SET NULL"), nullable=True)
    value: Mapped[str] = mapped_column(String(100), nullable=False)
    score: Mapped[int] = mapped_column(Integer, nullable=False)
    confidence: Mapped[float] = mapped_column(Float, default=0.0)
    drift: Mapped[float] = mapped_column(Float, default=0.0)

    user: Mapped["User"] = relationship(back_populates="value_scores")

