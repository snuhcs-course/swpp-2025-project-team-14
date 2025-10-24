from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List, Dict

from app.features.selfaware.models import Question, Answer, ValueScore, ValueMap
from app.features.selfaware.prompt import CAT_EN, CAT_KO

class JournalBase(BaseModel):
    title: str
    content: str
    keywords: Optional[str] = None
    gratitude: Optional[str] = None

class JournalCreate(JournalBase):
    user_id: int

class JournalUpdate(JournalBase):
    pass

class JournalRead(JournalBase):
    id: int
    user_id: int
    created_at: datetime

    class Config:
        orm_mode = True


class QuestionResponse(BaseModel):
    id: int
    question_type: str
    text: str
    categories_ko: Optional[List[str]] = None
    categories_ko: Optional[List[str]] = None
    created_at: datetime
    
    @staticmethod
    def from_question(question: Question) -> "QuestionResponse":
        return QuestionResponse(
            id=question.id,
            question_type=question.question_type,
            text=question.text,
            categories_ko=question.categories_ko,
            categories_en=question.categories_en,
            created_at=question.created_at
        )
    
class AnswerResponse(BaseModel):
    id: int
    question_id: int
    type: Optional[str] = None
    text: str
    created_at: datetime
    updated_at: datetime
    
    @staticmethod
    def from_answer(answer: Answer) -> "AnswerResponse":
        return AnswerResponse(
            id=answer.id,
            question_id=answer.question_id,
            type=answer.type,
            text=answer.text,
            created_at=answer.created_at,
            updated_at=answer.updated_at
        )
        
class QAResponse(BaseModel):
    question: QuestionResponse
    answer: Optional[AnswerResponse] = None


class QACursorResponse(BaseModel):
    items = List[QAResponse]
    next_cursor: int | None = Field(
        None, description="다음 페이지의 QA 데이터를 요청할 때 사용할 마지막 아이템의 ID"
    )
    
    @staticmethod
    def from_QAs(questions: List[Question], answers: List[Question]) -> "QACursorResponse":
        items = [
            QAResponse(
                question=QuestionResponse.from_question(q),
                answer=AnswerResponse.from_answer(a) if a else None
            ) for q, a in zip(questions, answers)
        ]
        next_cursor = items[-1].question.id if items else None

        return QACursorResponse(
            items=items,
            next_cursor=next_cursor
        )

    
class TopValueScoresResponse(BaseModel):
    value_scores = List[Dict]
    update_at: datetime
    
class ValueMapResponse(BaseModel):
    category_scores: List[Dict]
    update_at: datetime

    @staticmethod
    def from_value_map(value_map: ValueMap) -> "ValueMapResponse":
        category_scores = []
        for idx, cat_en in enumerate(CAT_EN):
            category_dict = {}
            category_dict["category_en"] = cat_en
            category_dict["category_ko"] = CAT_KO[cat_en]           
            category_dict["score"] = getattr(value_map, f"score_{idx}")
            category_scores.append(category_dict)
        
        return ValueMapResponse(
            category_scores=category_scores,
            update_at=value_map.updated_at
        )
        
class PersonalityInsightResponse(BaseModel):
    comment: str
    personality_insight: str
    update_at: datetime

    @staticmethod
    def from_value_map(value_map: ValueMap) -> "PersonalityInsightResponse":
        return PersonalityInsightResponse(
            comment=value_map.comment,
            personality_insight=value_map.personality_insight,
            update_at=value_map.updated_at
        )


class ValueScoreBase(BaseModel):
    answer_id: int
    user_id: int
    category: str
    value: str
    confidence: float
    intensity: float
    polarity: float

class ValueScore(ValueScoreBase):
    created_at: datetime

class ValueScoreCreate(ValueScoreBase):
    pass

class ValueScoreData(BaseModel):
    user_id: int
    category: str
    intensity: float

    model_config = {
        "from_attributes": True  # ✅ ORM 객체로부터 속성 추출 허용
    }

class TopValueScoreResponse(BaseModel):
    user_id: int
    value_scores: list
    updated_at: datetime
# ValueMap

class ValueMapBase(BaseModel):
    pass

class ValueMapCreate(ValueMapBase):
    user_id: int

class ValueMap(ValueMapBase):
    id: int
    user_id: int
    created_at: datetime

    class Config:
        orm_mode = True

class PersonalityInsightResponse(BaseModel):
    user_id: int
    personality_insight: str
    updated_at: datetime

class CategoryScore(BaseModel):
    category_ko: str
    category_en: str
    score: int