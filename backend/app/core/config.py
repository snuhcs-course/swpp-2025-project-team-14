import os

from pydantic_settings import BaseSettings, SettingsConfigDict

ENV = os.getenv("ENV", "local")
assert ENV in ("local", "prod")


class Settings(BaseSettings):
    APP_NAME: str = "MindLog"
    APP_ENV: str = "local"
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 3000
    APP_DEBUG: bool = True
    TIMEZONE: str = "Asia/Seoul"

    JWT_SECRET: str
    JWT_ALG: str = "HS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    JWT_REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    DB_HOST: str
    DB_PORT: str
    DB_USER: str
    DB_PASSWORD: str
    DB_NAME: str

    DATABASE_URL: str

    AWS_ACCESS_KEY_ID: str
    AWS_SECRET_ACCESS_KEY: str
    AWS_REGION: str = "ap-northeast-2"
    AWS_S3_BUCKET_NAME: str = "swpp-2025-team14"

    model_config = SettingsConfigDict(env_file=f".env.{ENV}", env_file_encoding="utf-8")


settings = Settings()
print(f"Loaded settings for {ENV} environment. Database URL: {settings.DATABASE_URL}")
