from collections.abc import AsyncIterator

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine
from sqlalchemy.pool import StaticPool

from app.core.config import Settings, get_settings
from app.core.dependencies import get_db
from app.core.security import hash_password
from app.main import app
from app.models import Base, Role, User


@pytest_asyncio.fixture
async def client() -> AsyncIterator[AsyncClient]:
    engine = create_async_engine(
        "sqlite+aiosqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool
    )
    factory = async_sessionmaker(engine, expire_on_commit=False)
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    settings = Settings(jwt_secret="test-secret-key-with-more-than-32-bytes", app_env="test")
    app.dependency_overrides[get_settings] = lambda: settings

    async def override_db():
        async with factory() as session:
            yield session

    app.dependency_overrides[get_db] = override_db
    async with factory() as session, session.begin():
        session.add_all(
            [
                User(
                    name="Administrator",
                    email="admin@example.com",
                    password_hash=hash_password("Admin-password-123!"),
                    role=Role.ADMIN,
                ),
                User(
                    name="Member",
                    email="user@example.com",
                    password_hash=hash_password("Member-password-123!"),
                    role=Role.USER,
                ),
            ]
        )
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as test_client:
        yield test_client
    app.dependency_overrides.clear()
    await engine.dispose()


@pytest.fixture
def anyio_backend() -> str:
    return "asyncio"
