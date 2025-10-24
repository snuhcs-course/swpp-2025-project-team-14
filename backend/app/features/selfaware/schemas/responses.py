from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List, Dict

# Journal

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

# Question

class QuestionBase(BaseModel):
    user_id: int
    text: str
    question_type: Optional[str] = "selfaware"

class QuestionCreate(QuestionBase):
    pass

class Question(QuestionBase):
    id: int
    created_at: datetime

    class Config:
        orm_mode = True

class QuestionDateResponse(QuestionBase):
    id: int
    categories_ko: Optional[List[str]] = None
    categories_en: Optional[List[str]] = None
    created_at: datetime

    model_config = {
        "from_attributes": True  # ✅ ORM 객체로부터 속성 추출 허용
    }

class QuestionGenerateRequest(BaseModel):
    journal_content: str
    user_id: int

# Answer

class AnswerBase(BaseModel):
    text: str

class AnswerCreate(AnswerBase):
    keywords: Optional[str] = None
    type: Optional[str] = None
    user_id: int
    question_id: int

class Answer(AnswerBase):
    id: int
    user_id: int
    question_id: int
    created_at: datetime
    keywords: Optional[str] = None
    type: Optional[str] = None

    model_config = {
        "from_attributes": True  # ✅ ORM 객체로부터 속성 추출 허용
    }

class AnswerDateResponse(AnswerBase):
    id: int
    user_id: int
    question_id: int
    created_at: datetime
    updated_at: datetime

    model_config = {
        "from_attributes": True  # ✅ ORM 객체로부터 속성 추출 허용
    }

class AnswerCreateRequest(AnswerCreate):
    pass

# ValueScore

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
    
# --- Combined ---
class QuestionWithAnswerResponse(BaseModel):
    question: QuestionDateResponse
    answer: Optional[AnswerDateResponse] = None

    class Config:
        orm_mode = True
