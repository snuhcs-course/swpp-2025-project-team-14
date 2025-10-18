from __future__ import annotations
from datetime import datetime
from typing import List, Optional

from sqlalchemy import String, Integer, Text, ForeignKey, DateTime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database.base import Base


class Journal(Base):
    __tablename__ = "journals"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)

    title: Mapped[str] = mapped_column(String(255), nullable=False)
    keywords: Mapped[Optional[str]] = mapped_column(Text, nullable=True)  # JSON 문자열로 저장
    content: Mapped[str] = mapped_column(Text, nullable=False)
    gratitude: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    # 관계 설정 (User <-> Journal)
    user: Mapped["User"] = relationship(back_populates="journals")  # N:1 (User → Journal)