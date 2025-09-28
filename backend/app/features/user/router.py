from typing import Annotated
from datetime import datetime
from fastapi import APIRouter, Depends, Cookie, Request
from fastapi import HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from app.database.session import get_db_session
from app.core.config import settings
from app.features.auth.service import AuthService
from app.features.auth.schemas import SignupRequest, LoginRequest, LogoutRequest, RefreshRequest, TokenResponse
from app.features.user.service import UserService

router = APIRouter(prefix='/user', tags=['user'])
security = HTTPBearer()

@router.get('/me', 
            status_code=200,
            summary="Get profile of user",
            description="Retrieve information about the currently authenticated user.",)
def me(
    authorization: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    auth_service: Annotated[AuthService, Depends()],
    user_service: Annotated[UserService, Depends()]
    ):
    token = authorization.credentials
    payload = auth_service.validate_access_token(token)
    login_id = payload.get('sub')
    
    user = user_service.get_user_by_login_id(login_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    return {
        "login_id": user.login_id,
        "username": user.username,
    }