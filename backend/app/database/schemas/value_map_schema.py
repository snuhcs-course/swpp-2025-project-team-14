from datetime import datetime
from pydantic import BaseModel
from typing import Dict

class ValueMapBase(BaseModel):
    value_map: Dict[str, float]

class ValueMapCreate(ValueMapBase):
    user_id: int

class ValueMapResponse(ValueMapBase):
    id: int
    user_id: int
    created_at: datetime

    class Config:
        orm_mode = True
