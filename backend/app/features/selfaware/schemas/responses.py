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
    type: Optional[str] = "selfaware"

class QuestionCreate(QuestionBase):
    pass

class Question(QuestionBase):
    id: int
    created_at: datetime

    class Config:
        orm_mode = True

class QuestionGenerateRequest(BaseModel):
    journal_content: str
    user_id: int

# Answer

class AnswerBase(BaseModel):
    text: str
    type: Optional[str] = None
    keywords: Optional[str] = None

class AnswerCreate(AnswerBase):
    user_id: int
    question_id: int

class Answer(AnswerBase):
    id: int
    user_id: int
    question_id: int
    created_at: datetime

    class Config:
        orm_mode = True

class AnswerCreateRequest(AnswerCreate):
    pass

# ValueMap

class ValueMapBase(BaseModel):
    value_map: Dict[str, float]

class ValueMapCreate(ValueMapBase):
    user_id: int

class ValueMap(ValueMapBase):
    id: int
    user_id: int
    created_at: datetime

    class Config:
        orm_mode = True