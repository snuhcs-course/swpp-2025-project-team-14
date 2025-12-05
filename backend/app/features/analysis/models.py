from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import JSON, DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.common.utilities import get_korea_time
from app.database.base import Base

if TYPE_CHECKING:
    from app.features.user.models import User


class Analysis(Base):
    __tablename__ = "analysis"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)

    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    user_type: Mapped[str | None] = mapped_column(
        String(50), nullable=True
    )  # 목표성취형

    neo_pi_score: Mapped[dict | None] = mapped_column(JSON, nullable=True)  # 확인 필요

    conscientiousness: Mapped[str | None] = mapped_column(Text, nullable=True)
    neuroticism: Mapped[str | None] = mapped_column(Text, nullable=True)
    extraversion: Mapped[str | None] = mapped_column(Text, nullable=True)
    openness: Mapped[str | None] = mapped_column(Text, nullable=True)
    agreeableness: Mapped[str | None] = mapped_column(Text, nullable=True)

    advice_type: Mapped[str | None] = mapped_column(
        String(50), nullable=True
    )  # 조언 이론 유형

    personalized_advice: Mapped[str | None] = mapped_column(Text, nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_korea_time, nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=get_korea_time,
        onupdate=get_korea_time,
        nullable=False,
    )

    user: Mapped[User] = relationship(back_populates="analysis")
