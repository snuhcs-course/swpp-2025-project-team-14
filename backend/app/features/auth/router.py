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


router = APIRouter(prefix='/auth', tags=['auth'])
security = HTTPBearer()

@router.post('/signup',
             status_code=201,
             summary="Sign up a new user",
             description="Create a new user account and return access and refresh tokens.",
             response_model=TokenResponse)
def signup(
    request: SignupRequest, 
    auth_service: Annotated[AuthService, Depends()],
):
    access, refresh = auth_service.signup(request.login_id, request.password, request.username)
    return TokenResponse(access=access, refresh=refresh)


@router.post('/login', 
             status_code=201,
             summary="Login a user",
             description="Log in an existing user and return access and refresh tokens.",
             response_model=TokenResponse)
def login(
    response: JSONResponse,
    request: LoginRequest, 
    auth_service: Annotated[AuthService, Depends()],
):
    access, refresh = auth_service.login(request.login_id, request.password)
    
    response.set_cookie(
        key="refresh_token",
        value=refresh,
        httponly=True,
        secure=True,
        expires=settings.JWT_REFRESH_TOKEN_EXPIRE_DAYS * 24 * 60 * 60,
        samesite="lax",
    )
    
    return TokenResponse(access=access, refresh=refresh)


@router.post('/logout',
             status_code=200,
             summary="Log out a user",
             description="Log out the current user and invalidate the refresh token.",
             response_model=TokenResponse)
def logout(
    request: Request,
    response: JSONResponse,
    auth_service: Annotated[AuthService, Depends()],
):
    refresh = request.cookies.get("refresh_token")
    if refresh is None:
        raise HTTPException(status_code=401, detail="Invalid refresh token")
    response.delete_cookie(key="refresh_token")
    auth_service.block_refresh_token(refresh, datetime.now())
    return "Success"


@router.post('/refresh',
             status_code=200,
             summary="Refresh access token",
             description="Refresh the access token using the refresh token.",
             response_model=TokenResponse)
def refresh(
    request: RefreshRequest, 
    response: JSONResponse,
    auth_service: Annotated[AuthService, Depends()]
):
    refresh = request.cookies.get("refresh_token")
    access, refresh = auth_service.refresh(refresh)
    response.set_cookie(
        key="refresh_token",
        value=refresh,
        httponly=True,
        secure=True,
        expires=settings.JWT_REFRESH_TOKEN_EXPIRE_DAYS * 24 * 60 * 60,
        samesite="lax",
    )
    return TokenResponse(access=access, refresh=refresh)