import logging
from contextlib import asynccontextmanager
from time import perf_counter
from uuid import uuid4

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.core.config import get_settings
from app.core.errors import error_body, install_exception_handlers
from app.database import engine
from app.routers import auth, tasks, users

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
logger = logging.getLogger("teamflow.http")
settings = get_settings()


@asynccontextmanager
async def lifespan(_: FastAPI):
    yield
    await engine.dispose()


app = FastAPI(
    title="Teamflow API",
    description="Командная доска задач с OAuth2, ролями и историей версий.",
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs" if settings.app_env.lower() == "dev" else None,
    redoc_url=None,
)
install_exception_handlers(app)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_credentials=False,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
    expose_headers=["X-Request-ID"],
)


@app.middleware("http")
async def request_logging(request: Request, call_next):
    request_id = request.headers.get("X-Request-ID", "")
    if len(request_id) > 80 or any(ord(character) < 32 for character in request_id):
        request_id = ""
    request_id = request_id or str(uuid4())
    request.state.request_id = request_id
    start = perf_counter()
    try:
        response = await call_next(request)
    except Exception:
        logger.exception("Unhandled HTTP failure request_id=%s", request_id)
        response = JSONResponse(
            status_code=500,
            content=error_body(request, 500, "Внутренняя ошибка. Сообщите администратору код запроса."),
        )
    response.headers["X-Request-ID"] = request_id
    logger.info(
        "method=%s path=%s status=%s duration_ms=%.1f request_id=%s",
        request.method,
        request.url.path,
        response.status_code,
        (perf_counter() - start) * 1000,
        request_id,
    )
    return response


app.include_router(auth.router)
app.include_router(users.router)
app.include_router(tasks.router)


@app.get("/health", include_in_schema=False)
@app.get("/actuator/health", include_in_schema=False)
async def health():
    return {"status": "UP"}
