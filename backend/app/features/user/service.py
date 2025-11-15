from datetime import date
from typing import Annotated

from fastapi import Depends

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
        password: str | None,
        username: str | None,
        gender: str | None,
        birthdate: date | None,
        appearance: str | None,
    ) -> None:
        if not any([password, username, gender, birthdate, appearance]):
            raise UserUpdateError()
        self.user_repository.update_me(
            user=user,
            password=password,
            username=username,
            gender=gender,
            birthdate=birthdate,
            appearance=appearance,
        )
