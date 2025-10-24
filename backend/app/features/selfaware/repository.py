from typing import Annotated, Optional, List, Sequence
from datetime import date, datetime, time, timedelta, timezone

from fastapi import Depends
from sqlalchemy import select, func, desc
from sqlalchemy.orm import Session

from app.database.session import get_db_session
from app.features.selfaware.models import Journal, Question, Answer, ValueMap, ValueScore


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

    def create_question(
        self, 
        user_id: int,
        question_type: str,
        text: str,        
        catergories_ko: Optional[List[str]] = None,
        catergories_en: Optional[List[str]] = None,
    ) -> Question:
        question = Question(
            user_id=user_id,
            question_type=question_type,
            text=text,
        )
        
        if catergories_ko and catergories_en:
            question.categories_ko = catergories_ko
            question.categories_en = catergories_en
        
        self.session.add(question)
        self.session.flush()
        return question

    def get_question_by_id(self, question_id: int) -> Question | None:
        return self.session.get(Question, question_id)

    def list_questions_by_user(
        self, user_id: int, limit: int = 10, cursor: int | None = None
    ) -> list[Question]:
        # cursor가 None이면 최신 글부터, cursor가 주어지면 해당 ID보다 작은 글부터
        query = (
            self.session.query(Question)
            .filter(Question.user_id == user_id)
            .order_by(Question.id.desc())
        )
        # cursor가 주어지면 해당 ID보다 작은 글부터
        if cursor is not None:
            query = query.filter(Question.id < cursor)
        # limit만큼 가져오기
        return query.limit(limit).all()

    def get_question_by_date(self, user_id: int, target_date: date) -> Question | None:
        # We store timestamps in UTC (models use timezone=True with utcnow),
        # so build a UTC day range: [start, end)
        start = datetime.combine(target_date, time.min, tzinfo=timezone.utc)
        end = start + timedelta(days=1)
        
        print(start, end)

        return self.session.scalar(
            select(Question)
            .where(
                Question.user_id == user_id,
                Question.created_at >= start,
                Question.created_at < end,
            )
            .order_by(Question.created_at.desc())
            .limit(1)
        )



# -------------------------------
# Answer Repository
# -------------------------------
class AnswerRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create_answer(
        self, 
        user_id: int,   
        question_id: int,
        text: str
    ) -> Answer:
        answer = Answer(
            user_id=user_id,
            question_id=question_id,
            text=text,
        )
        self.session.add(answer)
        self.session.flush()
        return answer

    def get_answer_by_id(self, answer_id: int) -> Optional[Answer]:
        return self.session.get(Answer, answer_id)

    def get_by_question(self, question_id: int) -> Optional[Answer]:
        return  (
            self.session.query(Answer)
            .filter(
                Answer.question_id == question_id
            )
            .first()
        )

    def get_by_user(self, user_id: int) -> Sequence[Answer]:
        return (
            self.session.query(Answer)
            .filter(
                Answer.user_id == user_id
            )
            .first()
        )
        
    def list_answers_by_user(self, user_id: int, question_ids: List[int]) -> list[Answer]:
        return (
            self.session.query(Answer)
            .filter(
                Answer.user_id == user_id,
                Answer.question_id.in_(question_ids)
            )
            .all()
        )

# -------------------------------
# ValueScore Repository
# -------------------------------
class ValueScoreRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def create_value_score(
        self, 
        user_id: int,
        question_id: int,
        answer_id: int,
        category: str,
        value: str,
        confidence: float,
        intensity: float,
        polarity: int,
        evidence_quotes: Optional[List[str]] = None,
    ) -> ValueScore:
        value_score = ValueScore(
            user_id=user_id,
            question_id=question_id,
            answer_id=answer_id,
            category=category,
            value=value,   
            confidence=confidence,
            intensity=intensity,
            polarity=polarity,
        )
        
        if evidence_quotes:
            value_score.evidence_quotes = evidence_quotes
        
        self.session.add(value_score)
        self.session.flush()
        return value_score
    
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

    def create_value_map(
        self, 
        user_id: int
    ) -> ValueMap:
        value_map = ValueMap(
            user_id=user_id,
        )
        self.session.add(value_map)
        self.session.flush()
        return value_map
    
    def update_by_value_score(
        self, 
        value_score: ValueScore
    ):
        value_map = (
            self.session.query(ValueMap)
            .filter(
                ValueMap.user_id == value_score.user_id
            )
            .first()
        )
        
        if not value_map:
            raise 

        if value_score.category == "Growth & Self-Actualization":
            value = (value_score.intensity + value_map.count_0 * value_map.score_0) // (value_map.count_0+1)
            count =  value_map.count_0 + 1
            category = 0
        elif value_score.category == "Relationships & Connection":
            value = (value_score.intensity + value_map.count_1 * value_map.score_1) // (value_map.count_1+1)
            count =  value_map.count_1 + 1
            category = 1
        elif value_score.category == "Security & Stability":
            value = (value_score.intensity + value_map.count_2 * value_map.score_2) // (value_map.count_2+1)
            count =  value_map.count_2 + 1
            category = 2
        elif value_score.category == "Freedom & Independence":
            value = (value_score.intensity + value_map.count_3 * value_map.score_3) // (value_map.count_3+1)
            count =  value_map.count_3 + 1
            category = 3
        elif value_score.category == "Achievement & Influence":
            value = (value_score.intensity + value_map.count_4 * value_map.score_4) // (value_map.count_4+1)
            count =  value_map.count_4 + 1
            category = 4
        elif value_score.category == "Enjoyment & Fulfillment":
            value = (value_score.intensity + value_map.count_5 * value_map.score_5) // (value_map.count_0+1)
            count =  value_map.count_5 + 1
            category = 5
        elif value_score.category == "Ethics & Transcendence":
            value = (value_score.intensity + value_map.count_6 * value_map.score_6) // (value_map.count_0+1)
            count =  value_map.count_6 + 1
            category = 6
        else:
            raise

        # 실제 객체에 반영
        setattr(value_map, f"score_{category}", value)
        setattr(value_map, f"count_{category}", count)

        # 갱신 시간 업데이트 (선택적)
        value_map.updated_at = datetime.utcnow()

        self.session.flush()
        return value_map

    def get_by_user(self, user_id: int) -> Optional[ValueMap]:
        return (
            self.session.query(ValueMap)
            .filter(
                ValueMap.user_id == user_id
            )
            .first()
        )
    
    def generate_comment(
        self, 
        user_id: int, 
        personality_insight: str, 
        comment: str
    ):
        value_map = self.get_by_user(user_id)
        if not value_map:
            raise

        setattr(value_map, "personality_insight", personality_insight)
        setattr(value_map, "comment", comment)

        self.session.flush()
        return value_map