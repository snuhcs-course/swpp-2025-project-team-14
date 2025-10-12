from __future__ import annotations

from datetime import datetime

from backend.app.database.models.emotion_model import Emotion
from backend.app.database.models.journal_image_model import JournalImage
from backend.app.features.user.models import User
from sqlalchemy import DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base


class Journal(Base):
    __tablename__ = "journals"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    title: Mapped[str] = mapped_column(String(255), nullable=False)
    keywords: Mapped[str | None] = mapped_column(
        Text, nullable=True
    )  # JSON 문자열로 저장
    content: Mapped[str] = mapped_column(Text, nullable=False)
    gratitude: Mapped[str | None] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    # 관계 설정 (User <-> Journal)
    user: Mapped[User] = relationship(back_populates="journals")  # N:1 (User → Journal)
    emotions: Mapped[Emotion | None] = relationship(
        back_populates="journal", cascade="all, delete-orphan"
    )  # 1:1 (Journal → Emotion)
    images: Mapped[JournalImage | None] = relationship(
        back_populates="journal", cascade="all, delete-orphan"
    )  # 1:1 (Journal → JournalImage)
