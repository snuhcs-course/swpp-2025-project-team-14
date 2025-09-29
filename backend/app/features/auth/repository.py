from typing import Annotated
from datetime import datetime
from fastapi import Depends
from sqlalchemy import select
from sqlalchemy.orm import Session
from app.database.session import get_db_session
from .models import BlockedToken


class BlockedTokenRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def add_blocked_token(self, token_id: str, expired_at: datetime) -> BlockedToken:
        blocked_token = BlockedToken(token_id=token_id, expired_at=expired_at)
        self.session.add(blocked_token)
        self.session.commit()
        self.session.refresh(blocked_token)
        return blocked_token

    def is_token_blocked(self, token_id: str) -> bool:
        return self.session.scalar(
            select(BlockedToken).filter(BlockedToken.token_id == token_id)
        ) is not None
