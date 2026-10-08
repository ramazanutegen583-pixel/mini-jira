from functools import lru_cache

from pydantic import SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

DEVELOPMENT_SECRET = "local-development-only-signing-key-replace-before-deploying-0123456789"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    app_name: str = "Teamflow API"
    app_env: str = "dev"
    database_url: str = "sqlite+aiosqlite:///./data/minijira.db"
    jwt_secret: SecretStr = SecretStr(DEVELOPMENT_SECRET)
    jwt_issuer: str = "teamflow"
    jwt_audience: str = "teamflow-api"
    access_token_minutes: int = 30
    admin_email: str = ""
    admin_password: SecretStr = SecretStr("")
    cors_origins: str = "http://127.0.0.1:4202,http://localhost:4202"

    @model_validator(mode="after")
    def validate_production_secrets(self) -> "Settings":
        if self.app_env.lower() == "production":
            secret = self.jwt_secret.get_secret_value()
            if secret == DEVELOPMENT_SECRET:
                raise ValueError("Set a unique JWT_SECRET before production startup.")
            if len(secret.encode()) < 32:
                raise ValueError("JWT_SECRET must contain at least 32 bytes.")
            if not self.database_url.startswith("postgresql+asyncpg://"):
                raise ValueError("Production requires a PostgreSQL DATABASE_URL.")
            if not self.admin_email or not self.admin_password.get_secret_value():
                raise ValueError("ADMIN_EMAIL and ADMIN_PASSWORD are required in production.")
            if len(self.admin_password.get_secret_value()) < 12:
                raise ValueError("ADMIN_PASSWORD must contain at least 12 characters.")
        return self

    @property
    def cors_origin_list(self) -> list[str]:
        return [origin.strip() for origin in self.cors_origins.split(",") if origin.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
