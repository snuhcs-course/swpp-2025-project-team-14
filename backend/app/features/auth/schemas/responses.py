from pydantic import BaseModel


class TokenResponse(BaseModel):
    access: str
    refresh: str

    @staticmethod
    def from_token(access: str, refresh: str) -> "TokenResponse":
        return TokenResponse(access=access, refresh=refresh)
