from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.security import hash_password
from app.models import Role, User
from app.schemas import CreateUser


async def get_by_id(session: AsyncSession, user_id: UUID) -> User | None:
    return await session.get(User, user_id)


async def get_by_email(session: AsyncSession, email: str) -> User | None:
    result = await session.execute(select(User).where(User.email == email.lower().strip()))
    return result.scalar_one_or_none()


async def list_users(session: AsyncSession) -> list[User]:
    result = await session.scalars(select(User).where(User.is_active.is_(True)).order_by(User.name, User.id))
    return list(result.all())


async def create_user(session: AsyncSession, data: CreateUser, *, role: Role | None = None) -> User:
    user = User(
        name=data.name,
        email=str(data.email).lower(),
        password_hash=hash_password(data.password),
        role=role or data.role,
    )
    session.add(user)
    await session.flush()
    await session.refresh(user)
    return user


async def email_exists(session: AsyncSession, email: str) -> bool:
    result = await session.scalar(
        select(func.count()).select_from(User).where(User.email == email.lower().strip())
    )
    return bool(result)
