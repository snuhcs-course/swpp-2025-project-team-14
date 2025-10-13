from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class AnswerBase(BaseModel):
    text: str
    type: Optional[str] = None
    keywords: Optional[str] = None

class AnswerCreate(AnswerBase):
    user_id: int
    question_id: int

class AnswerRead(AnswerBase):
    id: int
    user_id: int
    question_id: int
    created_at: datetime

    class Config:
        orm_mode = True