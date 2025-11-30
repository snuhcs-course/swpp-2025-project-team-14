from datetime import date
from typing import Annotated

from fastapi import Depends

from app.features.auth.security import verify_password
from app.features.user.errors import UserUpdateError
from app.features.user.models import User
from app.features.user.repository import UserRepository


class UserService:
    def __init__(
        self,
        user_repository: Annotated[UserRepository, Depends()],
    ) -> None:
        self.user_repository = user_repository

    def get_user_by_login_id(self, login_id: str) -> User | None:
        return self.user_repository.get_user_by_login_id(login_id)

    def update_me(
        self,
        user: User,
        username: str | None,
        gender: str | None,
        birthdate: date | None,
        appearance: str | None,
    ) -> None:
        if not any([username, gender, birthdate, appearance]):
            raise UserUpdateError()
        self.user_repository.update_me(
            user=user,
            username=username,
            gender=gender,
            birthdate=birthdate,
            appearance=appearance,
        )

    def is_my_password(self, user: User, current_password: str) -> bool:
        if not verify_password(current_password, user.hashed_password):
            return False
        return True

    def update_password(
        self,
        user: User,
        new_password: str,
    ) -> None:
        self.user_repository.update_me(user=user, password=new_password)
