from pydantic import BaseModel
from datetime import datetime
from typing import Optional

class QuestionBase(BaseModel):
    user_id: int
    text: str
    type: Optional[str] = "reflection"

class QuestionCreate(QuestionBase):
    pass

class Question(QuestionBase):
    id: int
    created_at: datetime

    class Config:
        orm_mode = True