from pathlib import Path

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.core.config import get_settings

settings = get_settings()
if settings.database_url.startswith("sqlite"):
    Path("data").mkdir(exist_ok=True)

engine = create_async_engine(
    settings.database_url, pool_pre_ping=not settings.database_url.startswith("sqlite")
)
SessionFactory = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)
