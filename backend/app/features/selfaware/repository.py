from typing import Annotated, Optional, List, Sequence
from fastapi import Depends
from sqlalchemy import select, func, desc
from sqlalchemy.orm import Session
from app.database.session import get_db_session
import schemas.responses as schema
from .models import Journal, Question, Answer, ValueMap, ValueScore
from datetime import date, datetime

# -------------------------------
# Journal Repository 테스트 용, merge 후 삭제 예정
# -------------------------------
class JournalRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def list_journals_by_user(
        self, user_id: int, limit: int = 10, cursor: int | None = None
    ) -> list[Journal]:
        # cursor가 None이면 최신 글부터, cursor가 주어지면 해당 ID보다 작은 글부터
        query = (
            self.session.query(Journal)
            .filter(Journal.user_id == user_id)
            .order_by(Journal.id.desc())
        )
        # cursor가 주어지면 해당 ID보다 작은 글부터
        if cursor is not None:
            query = query.filter(Journal.id < cursor)
        # limit만큼 가져오기
        return query.limit(limit).all()


# -------------------------------
# Question Repository
# -------------------------------
class QuestionRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create(self, question: schema.QuestionCreate) -> Question:
        db_question = Question(**question.dict())
        self.session.add(db_question)
        self.session.flush()
        self.session.refresh(db_question)
        return db_question

    def get(self, question_id: int) -> Optional[Question]:
        return self.session.scalar(
            select(Question).where(Question.id == question_id)
        )

    def get_by_user(self, user_id: int) -> Optional[Sequence[Question]]:
        return self.session.scalars(
            select(Question).where(Question.user_id == user_id)
        ).all()
    
    def get_by_date(self, target_date: date):
        return self.session.query(Question).filter(
            func.date(Question.created_at) == target_date
        )



# -------------------------------
# Answer Repository
# -------------------------------
class AnswerRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create(self, answer: schema.AnswerCreate) -> Answer:
        db_answer = Answer(**answer.dict())
        self.session.add(db_answer)
        self.session.flush()
        self.session.refresh(db_answer)
        return db_answer

    def get(self, id: int) -> Optional[Answer]:
        return self.session.scalar(
            select(Answer).where(Answer.id == id)
        )

    def get_by_question(self, question_id: int) -> Optional[Answer]:
        return self.session.scalar(
            select(Answer).where(Answer.question_id == question_id)
        )

    def get_by_user(self, user_id: int) -> Sequence[Answer]:
        return self.session.scalars(
            select(Answer).where(Answer.user_id == user_id)
        ).all()

# -------------------------------
# ValueScore Repository
# -------------------------------
class ValueScoreRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create(self, value_score_data: schema.ValueScoreCreate) -> ValueScore:
        db_value_score = ValueScore(**value_score_data.dict())
        self.session.add(db_value_score)
        self.session.flush()
        self.session.refresh(db_value_score)
        return db_value_score
    
    def get_top_5_value_scores(self, user_id: int) -> Optional[Sequence[ValueScore]]:
        self.session.scalars(
            select(ValueScore).where(ValueScore.user_id == user_id).order_by(desc(ValueScore.intensity)).limit(5)
        ).all()

# -------------------------------
# ValueMap Repository
# -------------------------------
class ValueMapRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create(self, value_map_data: schema.ValueMapCreate) -> ValueMap:
        db_value_map = ValueMap(**value_map_data.dict())
        self.session.add(db_value_map)
        self.session.flush()
        self.session.refresh(db_value_map)
        return db_value_map
    
    def update_by_value_score(self, value_score_data: schema.ValueScoreData):
        user_value_map = self.session.scalar(
            select(ValueMap)
            .where(ValueMap.user_id == value_score_data.user_id)
        )
        if not user_value_map:
            raise

        if value_score_data.category == "Growth & Self-Actualization":
            value = (value_score_data.intensity + user_value_map.count_0 * user_value_map.score_0) // (user_value_map.count_0+1)
            count =  user_value_map.count_0 + 1
            category = 0
        elif value_score_data.category == "Relationships & Connection":
            value = (value_score_data.intensity + user_value_map.count_1 * user_value_map.score_1) // (user_value_map.count_1+1)
            count =  user_value_map.count_1 + 1
            category = 1
        elif value_score_data.category == "Security & Stability":
            value = (value_score_data.intensity + user_value_map.count_2 * user_value_map.score_2) // (user_value_map.count_2+1)
            count =  user_value_map.count_2 + 1
            category = 2
        elif value_score_data.category == "Freedom & Independence":
            value = (value_score_data.intensity + user_value_map.count_3 * user_value_map.score_3) // (user_value_map.count_3+1)
            count =  user_value_map.count_3 + 1
            category = 3
        elif value_score_data.category == "Achievement & Influence":
            value = (value_score_data.intensity + user_value_map.count_4 * user_value_map.score_4) // (user_value_map.count_4+1)
            count =  user_value_map.count_4 + 1
            category = 4
        elif value_score_data.category == "Enjoyment & Fulfillment":
            value = (value_score_data.intensity + user_value_map.count_5 * user_value_map.score_5) // (user_value_map.count_0+1)
            count =  user_value_map.count_5 + 1
            category = 5
        elif value_score_data.category == "Ethics & Transcendence":
            value = (value_score_data.intensity + user_value_map.count_6 * user_value_map.score_6) // (user_value_map.count_0+1)
            count =  user_value_map.count_6 + 1
            category = 6
        else:
            raise

        # 실제 객체에 반영
        setattr(user_value_map, f"score_{category}", value)
        setattr(user_value_map, f"count_{category}", count)

        # 갱신 시간 업데이트 (선택적)
        user_value_map.updated_at = datetime.utcnow()

        # 커밋 및 새 값 반영
        self.session.add(user_value_map)
        self.session.flush()
        self.session.refresh(user_value_map)
        return user_value_map

    def get_by_user(self, user_id: int) -> Optional[ValueMap]:
        return self.session.scalar(
            select(ValueMap).where(ValueMap.user_id == user_id)
        )
    
    def generate_text(self, user_id: int, personality_insight: str, comment: str):
        user_value_map = self.session.scalar(
            select(ValueMap)
            .where(ValueMap.user_id == user_id)
        )
        if not user_value_map:
            raise

        # 실제 객체에 반영
        setattr(user_value_map, "personality_insight", personality_insight)
        setattr(user_value_map, "comment", comment)

        # 커밋 및 새 값 반영
        self.session.add(user_value_map)
        self.session.flush()
        self.session.refresh(user_value_map)
        return user_value_map