from pydantic import BaseModel
from datetime import datetime
from typing import Optional, List

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