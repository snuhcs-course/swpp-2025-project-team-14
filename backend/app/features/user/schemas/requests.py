from datetime import date
from typing import Annotated

from pydantic import BaseModel
from pydantic.functional_validators import AfterValidator

from app.features.auth.schemas.requests import (
    validate_birthdate,
    validate_gender,
    validate_password,
    validate_username,
)


class UpdateMeRequest(BaseModel):
    username: Annotated[str | None, AfterValidator(validate_username)] = None
    gender: Annotated[str | None, AfterValidator(validate_gender)] = None
    birthdate: Annotated[date | None, AfterValidator(validate_birthdate)] = None
    appearance: str | None = None


class UpdatePasswordRequest(BaseModel):
    current_password: str
    new_password: Annotated[str, AfterValidator(validate_password)]
