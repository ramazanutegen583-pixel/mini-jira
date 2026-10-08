from typing import Annotated

from fastapi import APIRouter, Depends

from app.core.dependencies import DbSession, require_role
from app.crud import users as users_crud
from app.exceptions import ApiError
from app.models import Role, User
from app.schemas import CreateUser, UserView

router = APIRouter(prefix="/api/users", tags=["team"])


@router.get("", response_model=list[UserView])
async def list_users(session: DbSession, _: Annotated[User, Depends(require_role(Role.ADMIN, Role.USER))]):
    return await users_crud.list_users(session)


@router.post("", response_model=UserView, status_code=201)
async def create_user(
    data: CreateUser,
    session: DbSession,
    _: Annotated[User, Depends(require_role(Role.ADMIN))],
) -> User:
    if await users_crud.email_exists(session, str(data.email)):
        raise ApiError(409, "Этот email уже используется")
    try:
        user = await users_crud.create_user(session, data)
        await session.commit()
        return user
    except Exception:
        await session.rollback()
        raise
