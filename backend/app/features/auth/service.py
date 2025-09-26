from typing import Annotated
from datetime import datetime
from fastapi import Depends
from fastapi import HTTPException
from app.core.config import settings
from app.features.auth.security import hash_password, verify_password, create_token, verify_token
from app.features.user.models import User
from app.features.user.repository import UserRepository
from app.features.auth.models import BlockedToken
from app.features.auth.repository import BlockedTokenRepository


class AuthService:
    def __init__(
        self,
        user_repository: Annotated[UserRepository, Depends()],
        blocked_token_repository: Annotated[BlockedTokenRepository, Depends()],
    ) -> None:
        self.user_repository = user_repository
        self.blocked_token_repository = blocked_token_repository
        
        
    def signup(self, login_id: str, password: str, username: str | None) -> tuple[str, str]:
        if username is None:
            username = login_id

        if self.user_repository.get_user_by_login_id(login_id) is not None:
            raise HTTPException(status_code=400, detail="Same login ID already exists")

        hashed_password = hash_password(password)
        user = self.user_repository.add_user(login_id, hashed_password, username)
        
        access_token = create_token(user.login_id, token_type='access')
        refresh_token = create_token(user.login_id, token_type='refresh')
        return access_token, refresh_token
    
    
    def login(self, login_id: str, password: str) -> tuple[str, str]:
        user = self.user_repository.get_user_by_login_id(login_id)
        if user is None or not verify_password(password, user.hashed_password):
            raise HTTPException(status_code=401, detail="Invalid login ID or password")
        
        access_token = create_token(user.login_id, token_type='access')
        refresh_token = create_token(user.login_id, token_type='refresh')
        return access_token, refresh_token
    
    def validate_access_token(self, token: str) -> str:
        payload = verify_token(token, expected_type='access')
        login_id = payload.get('sub')
        return login_id
    
    def validate_refresh_token(self, token: str) -> str:
        payload = verify_token(token, expected_type='refresh')
        
        # Check if the token is blocked
        if self.blocked_token_repository.is_token_blocked(token):
            raise HTTPException(status_code=401, detail="Token has been revoked")
        
        return payload
    
    def refresh(self, refresh: str) -> tuple[str, str]:
        payload = self.validate_refresh_token(refresh)
        login_id = payload.get('sub')
        
        new_access = create_token(login_id, token_type='access')
        new_refresh = create_token(login_id, token_type='refresh')
        
        # Block the old refresh token
        exp_timestamp = payload.get('exp')
        if exp_timestamp is None:
            raise HTTPException(status_code=401, detail="Invalid token: no exp")
        expired_at = datetime.fromtimestamp(exp_timestamp)
        self.blocked_token_repository.add_blocked_token(refresh, expired_at)

        return new_access, new_refresh

    def block_refresh_token(self, token: str, expired_at: datetime) -> None:
        self.blocked_token_repository.add_blocked_token(token, expired_at)