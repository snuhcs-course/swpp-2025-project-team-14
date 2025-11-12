from __future__ import annotations

from datetime import datetime, timezone, date
from typing import TYPE_CHECKING, List, Optional

from sqlalchemy import String, Integer, Text, ForeignKey, DateTime, JSON, Float, Date, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base
if TYPE_CHECKING:
    from app.features.user.models import User

from app.common.utilities import get_korea_time

def utcnow() -> datetime:
    return datetime.now(timezone.utc)

class Analysis(Base):
    __tablename__ = "analysis"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    user_type: Mapped[str | None] = mapped_column(String(50), nullable=True) # 목표성취형

    neo_pi_score:  Mapped[Optional[dict]] = mapped_column(JSON, nullable=True) # 확인 필요

    conscientiousness: Mapped[str | None] = mapped_column(Text, nullable=True)
    neuroticism: Mapped[str | None] = mapped_column(Text, nullable=True)
    extraversion: Mapped[str | None] = mapped_column(Text, nullable=True)
    openness: Mapped[str | None] = mapped_column(Text, nullable=True)
    agreeableness: Mapped[str | None] = mapped_column(Text, nullable=True)

    advice_type: Mapped[str | None] = mapped_column(String(50), nullable=True) # 조언 이론 유형

    personalized_advice: Mapped[str | None] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=get_korea_time, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=get_korea_time, onupdate=get_korea_time, nullable=False)

    user: Mapped[User] = relationship(back_populates="analysis")