import re
from typing import Annotated

from pydantic import AfterValidator, BaseModel

from app.common.errors import InvalidFieldFormatError

URL_PATTERN = re.compile(
    r"(https?://)?(www.)?[-a-zA-Z0-9@:%.+~#=]{2,256}.[a-z]{2,6}\b([-a-zA-Z0-9@:%+.~#?&//=]*)"
)


def validate_title(title: str) -> str:
    if not title.strip():
        raise InvalidFieldFormatError("title")
    if len(title) > 255:
        raise InvalidFieldFormatError("title")
    return title


def validate_content(content: str) -> str:
    if not content.strip():
        raise InvalidFieldFormatError("content")
    return content


def validate_url(value: list[str] | None) -> list[str] | None:
    if value is None:
        return value
    for url in value:
        if not re.match(URL_PATTERN, url):
            raise InvalidFieldFormatError("url")
    return value


class JournalCreateRequest(BaseModel):
    title: Annotated[str, AfterValidator(validate_title)]
    content: Annotated[str, AfterValidator(validate_content)]
    image_urls: Annotated[list[str] | None, AfterValidator(validate_url)] = None


class JournalUpdateRequest(BaseModel):
    title: Annotated[str | None, AfterValidator(validate_title)] = None
    content: Annotated[str | None, AfterValidator(validate_content)] = None
    image_url: Annotated[str | None, AfterValidator(validate_url)] = None
