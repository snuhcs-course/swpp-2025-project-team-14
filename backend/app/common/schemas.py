from pydantic import BaseModel
from typing import Optional, Any


class ErrorResponse(BaseModel):
    code: str
    message: str


class ResponseEnvelope(BaseModel):
    ok: bool = True
    data: Optional[Any] = None
    error: Optional[ErrorResponse] = None



    
