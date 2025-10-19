from typing import Annotated, Optional, List, Sequence
from fastapi import Depends
from sqlalchemy import select
from sqlalchemy.orm import Session
from app.database.session import get_db_session
import models as model
import schemas.responses as schema


# -------------------------------
# Journal Repository 테스트 용, merge 후 삭제 예정
# -------------------------------
class JournalRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create(self, journal: schema.JournalCreate) -> model.Journal:
        db_journal = model.Journal(**journal.dict())
        self.session.add(db_journal)
        self.session.flush()
        self.session.refresh(db_journal)
        return db_journal

    def get(self, journal_id: int) -> Optional[model.Journal]:
        return self.session.scalar(
            select(model.Journal).where(model.Journal.id == journal_id)
        )

    def get_by_user(self, user_id: int) -> Sequence[model.Journal]:
        return self.session.scalars(
            select(model.Journal).where(model.Journal.user_id == user_id)
        ).all()

    def update(self, journal_id: int, updates: schema.JournalUpdate) -> Optional[model.Journal]:
        db_journal = self.get(journal_id)
        if not db_journal:
            return None
        for key, value in updates.dict(exclude_unset=True).items():
            setattr(db_journal, key, value)
        self.session.flush()
        self.session.refresh(db_journal)
        return db_journal

    def delete(self, journal_id: int) -> Optional[model.Journal]:
        db_journal = self.get(journal_id)
        if db_journal:
            self.session.delete(db_journal)
            self.session.flush()
        return db_journal


# -------------------------------
# Question Repository
# -------------------------------
class QuestionRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create(self, question: schema.QuestionCreate) -> model.Question:
        db_question = model.Question(**question.dict())
        self.session.add(db_question)
        self.session.flush()
        self.session.refresh(db_question)
        return db_question

    def get(self, question_id: int) -> Optional[model.Question]:
        return self.session.scalar(
            select(model.Question).where(model.Question.id == question_id)
        )

    def get_by_user(self, user_id: int) -> Optional[Sequence[model.Question]]:
        return self.session.scalars(
            select(model.Question).where(model.Question.user_id == user_id)
        ).all()


# -------------------------------
# Answer Repository
# -------------------------------
class AnswerRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create(self, answer: schema.AnswerCreate) -> model.Answer:
        db_answer = model.Answer(**answer.dict())
        self.session.add(db_answer)
        self.session.flush()
        self.session.refresh(db_answer)
        return db_answer

    def get_by_question(self, question_id: int) -> Sequence[model.Answer]:
        return self.session.scalars(
            select(model.Answer).where(model.Answer.question_id == question_id)
        ).all()

    def get_by_user(self, user_id: int) -> Sequence[model.Answer]:
        return self.session.scalars(
            select(model.Answer).where(model.Answer.user_id == user_id)
        ).all()


# -------------------------------
# ValueMap Repository
# -------------------------------
class ValueMapRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create(self, value_map_data: schema.ValueMapCreate) -> model.ValueMap:
        db_value_map = model.ValueMap(**value_map_data.dict())
        self.session.add(db_value_map)
        self.session.flush()
        self.session.refresh(db_value_map)
        return db_value_map

    def get_latest(self, user_id: int) -> Optional[model.ValueMap]:
        return self.session.scalar(
            select(model.ValueMap)
            .where(model.ValueMap.user_id == user_id)
            .order_by(model.ValueMap.created_at.desc())
        )

    def get_by_user(self, user_id: int) -> Sequence[model.ValueMap]:
        return self.session.scalars(
            select(model.ValueMap)
            .where(model.ValueMap.user_id == user_id)
            .order_by(model.ValueMap.created_at.desc())
        ).all()
