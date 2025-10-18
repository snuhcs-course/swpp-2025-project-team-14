from typing import Annotated

from fastapi import (
    APIRouter,
    Depends,
    File,
    Form,
    Header,
    HTTPException,
    UploadFile,
    status,
)

from app.core.config import settings
from app.features.journal.schemas.responses import (
    JournalImageResponse,
    JournalImageResponseEnvelope,
)
from app.features.journal.service import JournalService

router = APIRouter(prefix="/webhook", tags=["webhook"])


@router.post(
    "/image-generation-complete",
    status_code=status.HTTP_200_OK,
    summary="Handle image generation completion webhook",
)
async def handle_image_generation_webhook(
    # multipart/form-data 필드
    journal_service: Annotated[JournalService, Depends()],
    job_id: str = Form(...),
    journal_id: int = Form(...),
    image_file: UploadFile = File(...),
    x_webhook_secret: str = Header(None),  # 보안 헤더
):
    # 웹훅 보안 검사
    if x_webhook_secret != settings.WEBHOOK_SECRET:
        raise HTTPException(
            status.HTTP_401_UNAUTHORIZED, "Invalid webhook secret token."
        )

    updated_image = await journal_service.process_image_generation_webhook(
        job_id=job_id, journal_id=journal_id, image_file=image_file
    )
    return JournalImageResponseEnvelope(
        data=JournalImageResponse.from_journal_image(updated_image)
    )
