from datetime import date
from typing import Annotated

from fastapi import Depends
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.database.session import get_db_session
from app.features.auth.security import hash_password

from .models import User


class UserRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def add_user(
        self,
        login_id: str,
        hashed_password: str,
        username: str,
        gender: str,
        birthdate: date,
    ) -> User:
        user = User(
            login_id=login_id,
            hashed_password=hashed_password,
            username=username,
            gender=gender,
            birthdate=birthdate,
            appearance=None,
        )
        self.session.add(user)
        return user

    def get_user_by_login_id(self, login_id: str) -> User | None:
        return self.session.scalar(select(User).filter(User.login_id == login_id))

    def update_me(
        self,
        user: User,
        password: str | None = None,
        username: str | None = None,
        gender: str | None = None,
        birthdate: date | None = None,
        appearance: str | None = None,
    ) -> None:
        if password is not None:
            user.hashed_password = hash_password(password)
        if username is not None:
            user.username = username
        if gender is not None:
            user.gender = gender
        if birthdate is not None:
            user.birthdate = birthdate
        if appearance is not None:
            user.appearance = appearance
        self.session.flush()
