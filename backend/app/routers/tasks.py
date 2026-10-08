from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, Query, Response, status

from app.core.dependencies import CurrentUser, DbSession, require_role
from app.crud import tasks as tasks_crud
from app.models import Role, TaskStatus, User
from app.schemas import ColumnView, TaskCreate, TaskMove, TaskPage, TaskUpdate, TaskView

router = APIRouter(prefix="/api", tags=["board"])


@router.get("/tasks", response_model=TaskPage)
async def list_tasks(
    session: DbSession,
    _: Annotated[User, Depends(require_role(Role.ADMIN, Role.USER))],
    page: int = Query(default=0, ge=0),
    size: int = Query(default=100, ge=1, le=100),
) -> TaskPage:
    return await tasks_crud.list_tasks(session, page, size)


@router.post("/tasks", response_model=TaskView, status_code=status.HTTP_201_CREATED)
async def create_task(data: TaskCreate, session: DbSession, user: CurrentUser) -> TaskView:
    return await tasks_crud.create_task(session, data, user)


@router.put("/tasks/{task_id}", response_model=TaskView)
async def update_task(task_id: UUID, data: TaskUpdate, session: DbSession, _: CurrentUser) -> TaskView:
    return await tasks_crud.update_task(session, task_id, data)


@router.patch("/tasks/{task_id}/status", response_model=TaskView)
async def move_task(task_id: UUID, data: TaskMove, session: DbSession, _: CurrentUser) -> TaskView:
    return await tasks_crud.move_task(session, task_id, data)


@router.delete("/tasks/{task_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_task(
    task_id: UUID,
    session: DbSession,
    _: Annotated[User, Depends(require_role(Role.ADMIN))],
    version: int = Query(ge=0),
) -> Response:
    await tasks_crud.delete_task(session, task_id, version)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get("/statuses", response_model=list[ColumnView])
async def statuses(_: CurrentUser) -> list[ColumnView]:
    return [ColumnView(id=value, label=value.label) for value in TaskStatus]
