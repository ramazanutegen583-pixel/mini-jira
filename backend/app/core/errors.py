from datetime import UTC, datetime

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy.exc import IntegrityError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.logging import logger
from app.exceptions import ApiError


def error_body(request: Request, status_code: int, message: str, *, fields: dict | None = None) -> dict:
    return {
        "timestamp": datetime.now(UTC).isoformat(),
        "status": status_code,
        "message": message,
        "requestId": getattr(request.state, "request_id", None),
        "fields": fields or {},
    }


def install_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(RequestValidationError)
    async def validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
        fields: dict[str, str] = {}
        for item in exc.errors():
            location = ".".join(str(part) for part in item["loc"] if part not in {"body", "query", "path"})
            fields.setdefault(location or "request", item["msg"])
        return JSONResponse(
            status_code=422, content=error_body(request, 422, "Проверьте введённые данные", fields=fields)
        )

    @app.exception_handler(ApiError)
    async def api_error(request: Request, exc: ApiError) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code, content=error_body(request, exc.status_code, exc.message)
        )

    @app.exception_handler(StarletteHTTPException)
    async def http_error(request: Request, exc: StarletteHTTPException) -> JSONResponse:
        defaults = {403: "Недостаточно прав", 404: "Ресурс не найден"}
        message = str(exc.detail) if exc.status_code < 500 else "Внутренняя ошибка сервера"
        return JSONResponse(
            status_code=exc.status_code,
            content=error_body(request, exc.status_code, defaults.get(exc.status_code, message)),
            headers=dict(exc.headers or {}),
        )

    @app.exception_handler(IntegrityError)
    async def integrity_error(request: Request, exc: IntegrityError) -> JSONResponse:
        return JSONResponse(
            status_code=409, content=error_body(request, 409, "Запись конфликтует с существующими данными")
        )

    @app.exception_handler(Exception)
    async def unexpected_error(request: Request, exc: Exception) -> JSONResponse:
        logger.exception(
            "Unhandled request failure request_id=%s", getattr(request.state, "request_id", None)
        )
        return JSONResponse(
            status_code=500,
            content=error_body(request, 500, "Внутренняя ошибка. Сообщите администратору код запроса."),
        )
