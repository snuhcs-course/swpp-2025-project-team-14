from __future__ import annotations

from datetime import datetime, timezone, date
from typing import TYPE_CHECKING, List, Optional

from sqlalchemy import String, Integer, Text, ForeignKey, DateTime, JSON, Float, Date, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base
if TYPE_CHECKING:
    from app.features.user.models import User

def utcnow() -> datetime:
    return datetime.now(timezone.utc)

class Analysis(Base):
    __tablename__ = "analysis"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    user_type: Mapped[str | None] = mapped_column(String(50), nullable=True) # 목표성취형

    neo_pi_score:  Mapped[Optional[List[int]]] = mapped_column(JSON, nullable=True) # 확인 필요

    comprehensive_analysis: Mapped[str] = mapped_column(Text, nullable=True)

    advice_type: Mapped[str | None] = mapped_column(String(50), nullable=True) # 조언 이론 유형

    personalized_advice: Mapped[str] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, nullable=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow, nullable=False)

    user: Mapped[User] = relationship(back_populates="analysis")