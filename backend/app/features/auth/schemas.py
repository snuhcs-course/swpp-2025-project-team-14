from pydantic import BaseModel


class SignupRequest(BaseModel):
    login_id: str
    password: str
    username: str | None = None
    
    if username is None:
        username = "익명의 사용자"
        
class LoginRequest(BaseModel):
    login_id: str
    password: str

class LogoutRequest(BaseModel):
    refresh: str
    
class RefreshRequest(BaseModel):
    refresh: str
class LogoutResponse(BaseModel):
    pass
class TokenResponse(BaseModel):
    access: str
    refresh: str
    
class RefreshTokenResponse(BaseModel):
    refresh: str
class AccessTokenResponse(BaseModel):
    access: str