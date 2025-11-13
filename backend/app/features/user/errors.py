from fastapi import HTTPException


class UserUpdateError(HTTPException):
    def __init__(self) -> None:
        super().__init__(
            status_code=400,
            detail="At least one field must be provided for update user",
        )
