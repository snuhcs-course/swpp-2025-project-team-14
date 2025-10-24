from pydantic import BaseModel, Field
from typing import Optional


class AnswerRequest(BaseModel):
    question_id: int
    answer: str