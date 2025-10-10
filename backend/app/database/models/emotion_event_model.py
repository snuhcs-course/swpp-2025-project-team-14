from datetime import datetime
from sqlalchemy import Integer, String, ForeignKey, DateTime, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database.base import Base

class EmotionEvent(Base):
    __tablename__ = "emotion_events"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    period_type: Mapped[str] = mapped_column(String(50), nullable=False)
    emotion: Mapped[str] = mapped_column(String(50), nullable=False)
    journals: Mapped[str | None] = mapped_column(Text, nullable=True)   # JSON 문자열
    summaries: Mapped[str | None] = mapped_column(Text, nullable=True)  # JSON 문자열
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    user: Mapped["User"] = relationship(back_populates="emotion_events")