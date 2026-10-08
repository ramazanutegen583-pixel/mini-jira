from datetime import UTC, datetime, timedelta
from uuid import uuid4

import jwt
from pwdlib import PasswordHash
from pwdlib.exceptions import UnknownHashError

from app.core.config import Settings
from app.models import User

password_hash = PasswordHash.recommended()
_DUMMY_HASH = password_hash.hash("nonexistent-account-password-timing-equalizer")


def hash_password(password: str) -> str:
    return password_hash.hash(password)


def verify_password(password: str, hashed_password: str | None) -> bool:
    try:
        return password_hash.verify(password, hashed_password or _DUMMY_HASH)
    except (UnknownHashError, ValueError):
        return False


def create_access_token(user: User, settings: Settings) -> tuple[str, int]:
    expires_in = settings.access_token_minutes * 60
    issued_at = datetime.now(UTC)
    claims = {
        "sub": str(user.id),
        "role": user.role.value,
        "iss": settings.jwt_issuer,
        "aud": settings.jwt_audience,
        "iat": issued_at,
        "exp": issued_at + timedelta(seconds=expires_in),
        "jti": str(uuid4()),
    }
    return jwt.encode(claims, settings.jwt_secret.get_secret_value(), algorithm="HS256"), expires_in


def decode_access_token(token: str, settings: Settings) -> dict:
    return jwt.decode(
        token,
        settings.jwt_secret.get_secret_value(),
        algorithms=["HS256"],
        issuer=settings.jwt_issuer,
        audience=settings.jwt_audience,
        options={"require": ["sub", "iss", "aud", "iat", "exp", "jti", "role"]},
        leeway=5,
    )
