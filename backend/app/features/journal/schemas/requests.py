import re
from typing import Annotated

from pydantic import AfterValidator, BaseModel

from app.common.errors import InvalidFieldFormatError

URL_PATTERN = re.compile(
    r"(https?://)?(www.)?[-a-zA-Z0-9@:%.+~#=]{2,256}.[a-z]{2,6}\b([-a-zA-Z0-9@:%+.~#?&//=]*)"
)

ALLOWED_IMAGE_TYPES = {
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/webp",
}

ALLOWED_EMOTIONS = {
    "happy",
    "sad",
    "anxious",
    "calm",
    "annoyed",
    "satisfied",
    "bored",
    "interested",
    "lethargic",
    "energetic",
}


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


def validate_filename(filename: str) -> str:
    stripped_filename = filename.strip()
    if not stripped_filename:
        raise InvalidFieldFormatError("filename cannot be empty")

    if len(stripped_filename) > 255:
        raise InvalidFieldFormatError("filename is too long")

    if "/" in stripped_filename or "\\" in stripped_filename:
        raise InvalidFieldFormatError("filename cannot contain path separators")

    if "." not in stripped_filename or stripped_filename.startswith("."):
        raise InvalidFieldFormatError("filename must have a valid extension")

    return stripped_filename


def validate_content_type(content_type: str) -> str:
    normalized_type = content_type.strip().lower()
    if normalized_type not in ALLOWED_IMAGE_TYPES:
        raise InvalidFieldFormatError(
            f"Unsupported content_type. Allowed types are: {', '.join(ALLOWED_IMAGE_TYPES)}"
        )
    return normalized_type


def validate_emotions(value: dict[str, int]) -> dict[str, int]:
    # 1. 감정 키(key)가 허용된 10가지 종류인지 확인
    if not set(value.keys()).issubset(ALLOWED_EMOTIONS):
        raise ValueError("Invalid emotion keys provided.")

    # 2. 10가지 감정이 모두 포함되었는지 확인
    if len(value) != len(ALLOWED_EMOTIONS):
        raise ValueError("All 10 emotion values must be provided.")

    # 3. 감정 강도(intensity)가 유효한 범위(0-4)인지 확인
    for intensity in value.values():
        if not 0 <= intensity <= 4:  # 강도 범위를 0-4로 가정
            raise ValueError("Emotion intensity must be between 0 and 4.")

    return value


class JournalCreateRequest(BaseModel):
    title: Annotated[str, AfterValidator(validate_title)]
    content: Annotated[str, AfterValidator(validate_content)]
    emotions: Annotated[dict[str, int], AfterValidator(validate_emotions)]
    image_urls: Annotated[list[str] | None, AfterValidator(validate_url)] = None


class JournalUpdateRequest(BaseModel):
    title: Annotated[str | None, AfterValidator(validate_title)] = None
    content: Annotated[str | None, AfterValidator(validate_content)] = None
    image_url: Annotated[str | None, AfterValidator(validate_url)] = None


class ImageUploadRequest(BaseModel):
    """클라이언트가 이미지 업로드를 요청할 때 보내는 데이터"""

    filename: Annotated[str, AfterValidator(validate_filename)]
    content_type: Annotated[str, AfterValidator(validate_content_type)]


class ImageCompletionRequest(BaseModel):
    """ "클라이언트가 이미지 업로드 완료를 알릴 때 보내는 데이터"""

    image_url: Annotated[str, AfterValidator(validate_url)]
    s3_key: str


class ImageGenerateRequest(BaseModel):
    """클라이언트가 이미지 생성을 요청할 때 보내는 데이터"""

    journal_id: int
    prompt_text: str


class ImageGenerateResponse(BaseModel):
    """이미지 생성 요청 후 클라이언트에게 반환하는 데이터"""

    job_id: str
    status: str


class ImageWebhookResponse(BaseModel):
    """웹훅 처리 완료 후 GPU 서버에게 반환하는 데이터"""

    status: str
    image_url: str
