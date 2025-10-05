import os
from pydantic_settings import BaseSettings, SettingsConfigDict

ENV = os.getenv("ENV", "prod")
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
    JWT_EXPIRE_MINUTES: int = 60
    
    DB_HOST: str
    DB_PORT: int
    DB_USER: str
    DB_PASSWORD: str
    DB_NAME: str
    
    DATABASE_URL: str

    model_config = SettingsConfigDict(env_file=f".env.{ENV}", env_file_encoding="utf-8")


settings = Settings()