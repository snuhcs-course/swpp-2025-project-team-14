from datetime import datetime
from sqlalchemy import Integer, String, ForeignKey, DateTime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database.base import Base

class JournalImage(Base):
    __tablename__ = "journal_images"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    journal_id: Mapped[int] = mapped_column(ForeignKey("journals.id", ondelete="CASCADE"), nullable=False)
    image_url: Mapped[str] = mapped_column(String(512), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    journal: Mapped["Journal"] = relationship(back_populates="images")