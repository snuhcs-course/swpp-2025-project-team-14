from typing import Annotated

from fastapi import APIRouter, Depends
from fastapi.security import HTTPBearer

from app.common.authorization import get_current_user
from app.features.user.models import User
from app.features.user.schemas.requests import UpdateMeRequest
from app.features.user.schemas.responses import ProfileResponse
from app.features.user.service import UserService

router = APIRouter(prefix="/user", tags=["user"])
security = HTTPBearer()


@router.get(
    "/me",
    status_code=200,
    summary="Get profile of user",
    description="Retrieve information about the currently authenticated user.",
    response_model=ProfileResponse,
)
def me(
    user: User = Depends(get_current_user),
):
    return ProfileResponse.from_profile(user)


@router.patch(
    "/me",
    status_code=200,
    summary="Update profile of user",
    description="Update the currently authenticated user data and retrieve info.",
)
def update_me(
    request: UpdateMeRequest,
    user_service: Annotated[UserService, Depends()],
    user: User = Depends(get_current_user),
) -> str:
    user_service.update_me(
        user,
        request.password,
        request.username,
        request.gender,
        request.birthdate,
        request.appearance,
    )
    return "Update Success"
