from datetime import date

from pydantic import BaseModel

from app.features.user.models import User


class ProfileResponse(BaseModel):
    id: int
    login_id: str
    username: str
    gender: str
    birthdate: date
    appearance: str | None

    @staticmethod
    def from_profile(user: User) -> "ProfileResponse":
        return ProfileResponse(
            id=user.id,
            login_id=user.login_id,
            username=user.username,
            gender=user.gender,
            birthdate=user.birthdate,
            appearance=user.appearance,
        )
