from __future__ import annotations

from datetime import date
from typing import TYPE_CHECKING

from sqlalchemy import Date, Integer, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base

if TYPE_CHECKING:
    from app.features.analysis.models import Analysis
    from app.features.journal.models import Journal
    from app.features.selfaware.models import Answer, Question, ValueMap


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    login_id: Mapped[str] = mapped_column(String(50), unique=True, nullable=False)
    hashed_password: Mapped[str] = mapped_column(String(100), nullable=False)
    username: Mapped[str] = mapped_column(String(100), nullable=True)
    gender: Mapped[str] = mapped_column(String(50), nullable=False)
    birthdate: Mapped[date] = mapped_column(Date, nullable=False)
    appearance: Mapped[str | None] = mapped_column(String(100), nullable=True)

    journals: Mapped[list[Journal]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    questions: Mapped[list[Question]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    answers: Mapped[list[Answer]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    value_maps: Mapped[list[ValueMap]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )
    analysis: Mapped[list[Analysis]] = relationship(
        back_populates="user", cascade="all, delete-orphan"
    )

    @property
    def age(self) -> int:
        today = date.today()
        return (
            today.year
            - self.birthdate.year
            - ((today.month, today.day) < (self.birthdate.month, self.birthdate.day))
        )
