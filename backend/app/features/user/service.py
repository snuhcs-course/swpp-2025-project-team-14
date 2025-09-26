from typing import Annotated
from fastapi import Depends
from app.core.config import settings
from app.features.user.models import User
from app.features.user.repository import UserRepository


class UserService:
    def __init__(
        self,
        user_repository: Annotated[UserRepository, Depends()],
    ) -> None:
        self.user_repository = user_repository
