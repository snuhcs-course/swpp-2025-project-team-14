from datetime import datetime
from sqlalchemy import Integer, String, ForeignKey, DateTime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database.base import Base

class Emotion(Base):
    __tablename__ = "emotions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    journal_id: Mapped[int] = mapped_column(ForeignKey("journals.id", ondelete="CASCADE"), nullable=False)
    emotion: Mapped[str] = mapped_column(String(50), nullable=False)
    intensity: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    journal: Mapped["Journal"] = relationship(back_populates="emotions")