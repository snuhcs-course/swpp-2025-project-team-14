from typing import Annotated

from pydantic import BaseModel
from pydantic.functional_validators import AfterValidator

from app.features.auth.schemas.requests import (
    validate_age,
    validate_gender,
    validate_password,
    validate_username,
)


class UpdateMeRequest(BaseModel):
    password: Annotated[str | None, AfterValidator(validate_password)] = None
    username: Annotated[str | None, AfterValidator(validate_username)] = None
    gender: Annotated[str | None, AfterValidator(validate_gender)] = None
    age: Annotated[int | None, AfterValidator(validate_age)] = None
    appearance: str | None = None
