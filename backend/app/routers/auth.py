from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from starlette.concurrency import run_in_threadpool

from app.core.config import Settings, get_settings
from app.core.dependencies import CurrentUser, DbSession
from app.core.security import create_access_token, verify_password
from app.crud.users import get_by_email
from app.models import User
from app.schemas import OAuthSession, UserView

router = APIRouter(prefix="/api/auth", tags=["authentication"])


@router.post(
    "/login", response_model=OAuthSession, summary="OAuth2 password flow: email и пароль → Bearer JWT"
)
async def login(
    credentials: Annotated[OAuth2PasswordRequestForm, Depends()],
    session: DbSession,
    settings: Annotated[Settings, Depends(get_settings)],
) -> dict:
    user = await get_by_email(session, credentials.username)
    valid = await run_in_threadpool(
        verify_password, credentials.password, user.password_hash if user else None
    )
    if user is None or not valid or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Неверный email или пароль",
            headers={"WWW-Authenticate": "Bearer"},
        )
    token, expires_in = create_access_token(user, settings)
    return {"access_token": token, "token_type": "bearer", "expires_in": expires_in, "user": user}


@router.get("/me", response_model=UserView, summary="Получить текущего пользователя")
async def me(user: CurrentUser) -> User:
    return user
