import re
from datetime import date
from typing import Annotated

from pydantic import BaseModel
from pydantic.functional_validators import AfterValidator

from app.common.errors import InvalidFieldFormatError

USERNAME_PATTERN = re.compile(r"^[a-zA-Z0-9가-힣_-]{3,20}$")
LOGIN_ID_PATTERN = re.compile(r"^[a-zA-Z0-9_.]{6,20}$")
GENDER = {"Female", "Male"}


def validate_username(value: str | None) -> str | None:
    # username should be 3 to 20 characters long and can only contain letters, numbers, underscores (_), and hyphens (-)
    if value is None:
        return value
    if not re.match(USERNAME_PATTERN, value):
        raise InvalidFieldFormatError("username")
    return value


def validate_login_id(value: str | None) -> str | None:
    # login_id should be 6 to 20 characters long and can only contain letters, numbers, underscores (_), and dots (.)
    if value is None:
        return value
    if not re.match(LOGIN_ID_PATTERN, value):
        raise InvalidFieldFormatError("Login_id")
    return value


def validate_password(value: str | None) -> str | None:
    # password should be 8 to 20 characters long and must include at least two of the following: uppercase letters, lowercase letters, digits, special characters
    if value is None:
        return value
    if len(value) < 8 or len(value) > 20:
        raise InvalidFieldFormatError("password")

    contains_uppercase = False
    contains_lowercase = False
    contains_digit = False
    contains_special = False

    for char in value:
        if char.isupper():
            contains_uppercase = True
        elif char.islower():
            contains_lowercase = True
        elif char.isdigit():
            contains_digit = True
        else:
            contains_special = True

    constraints_cardinality = sum(
        [contains_uppercase, contains_lowercase, contains_digit, contains_special]
    )
    if constraints_cardinality < 2:
        raise InvalidFieldFormatError("password")

    return value


def validate_gender(value: str) -> str:
    if value not in GENDER:
        raise InvalidFieldFormatError("gender")
    return value


def validate_birthdate(value: date) -> date:
    today = date.today()
    if value > today:
        raise InvalidFieldFormatError("birthdate")
    if value.year < 1900:
        raise InvalidFieldFormatError("birthdate")
    return value


class SignupRequest(BaseModel):
    login_id: Annotated[str, AfterValidator(validate_login_id)]
    password: Annotated[str, AfterValidator(validate_password)]
    username: Annotated[str, AfterValidator(validate_username)]
    gender: Annotated[str, AfterValidator(validate_gender)]
    birthdate: Annotated[date, AfterValidator(validate_birthdate)]


class LoginRequest(BaseModel):
    login_id: str
    password: str


class RefreshTokenRequest(BaseModel):
    refresh: str
