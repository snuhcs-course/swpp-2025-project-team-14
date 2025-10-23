from __future__ import annotations
from datetime import datetime, timezone
from typing import List, Optional
from sqlalchemy import String, Integer, Text, ForeignKey, DateTime, JSON, Float
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database.base import Base
from app.features.user.models import User

def utcnow() -> datetime:
    return datetime.now(timezone.utc)


# Journal class는 테스트 용 merge 후 삭제 예정
class Journal(Base):
    __tablename__ = "journals"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    title: Mapped[str] = mapped_column(String(255), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    summary: Mapped[str | None] = mapped_column(Text, nullable=True)
    gratitude: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)

    # (Many to One)
    user: Mapped[User] = relationship(back_populates="journals")

class Question(Base):
    __tablename__ = "questions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)

    question_type: Mapped[str | None] = mapped_column(String(50), nullable=True) # single_category | multi_category | personalized_category
    text: Mapped[str] = mapped_column(Text, nullable=False)

    categories_ko: Mapped[Optional[List[str]]] = mapped_column(JSON, nullable=True)
    categories_en: Mapped[Optional[List[str]]] = mapped_column(JSON, nullable=True)
    
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)

    user: Mapped["User"] = relationship(back_populates="questions")
    answers: Mapped["Answer"] = relationship(back_populates="question", uselist=False, cascade="all, delete_orphan")

class Answer(Base):
    __tablename__ = "answers"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    question_id: Mapped[int] = mapped_column(ForeignKey("questions.id", ondelete="CASCADE"), nullable=False, unique=True, index=True)

    type: Mapped[str | None] = mapped_column(String(50), nullable=True) # single_category | multi_category | personalized_category
    text: Mapped[str] = mapped_column(Text, nullable=False)

    keywords: Mapped[Optional[List[str]]] = mapped_column(JSON, nullable=True) # 확인 필요

    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)

    user: Mapped["User"] = relationship(back_populates="answers")
    question: Mapped["Question"] = relationship(back_populates="answers")
    
    value_scores: Mapped[List["ValueScore"]] = relationship(
        back_populates="answer",
        cascade="all, delete-orphan",
        passive_deletes=True
    )

class ValueScore(Base):
    __tablename__ = "value_scores"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    answer_id: Mapped[int] = mapped_column(ForeignKey("answers.id", ondelete="SET NULL"), nullable=False, index=True)

    user_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)

    question_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True) # Doc에 명시가 안되어 있는데, service에서 사용할 가능성 있어 남겨둠, 확정시 문서에 추가
    
    category: Mapped[str] = mapped_column(String(64), nullable=False)  # 대분류
    value: Mapped[str] = mapped_column(String(80), nullable=False)  # 소분류

    confidence: Mapped[float] = mapped_column(Float, nullable=False)               # 0..1
    intensity: Mapped[float]  = mapped_column(Float, nullable=False)               # 0..1
    polarity:  Mapped[int]    = mapped_column(Integer, nullable=False, default=1)  # -1, 0, +1       # 서버 계산치(즉시/증분)

    # 증거(문장/구) — 최대 2개 저장 추천
    evidence_quotes: Mapped[Optional[List[str]]] = mapped_column(JSON)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    
    answer: Mapped["Answer"] = relationship(back_populates="value_scores")

# 추가 수정 필요
class ValueMap(Base):
    __tablename__ = "value_maps"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    value_map: Mapped[dict] = mapped_column(JSON, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)

    user: Mapped["User"] = relationship(back_populates="value_maps")



