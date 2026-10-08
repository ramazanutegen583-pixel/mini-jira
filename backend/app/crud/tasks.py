from math import ceil
from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload
from sqlalchemy.orm.exc import StaleDataError

from app.exceptions import ApiError
from app.models import Task, User
from app.schemas import TaskCreate, TaskMove, TaskPage, TaskUpdate, TaskView


async def list_tasks(session: AsyncSession, page: int, size: int) -> TaskPage:
    total = await session.scalar(select(func.count()).select_from(Task)) or 0
    statement = (
        select(Task)
        .options(selectinload(Task.assignee), selectinload(Task.reporter))
        .order_by(Task.created_at.desc(), Task.id)
        .offset(page * size)
        .limit(size)
    )
    tasks = list((await session.scalars(statement)).all())
    return TaskPage(
        content=[TaskView.model_validate(task) for task in tasks],
        total_elements=total,
        total_pages=ceil(total / size),
        number=page,
    )


async def _get_for_response(session: AsyncSession, task_id: UUID) -> Task:
    result = await session.execute(
        select(Task)
        .options(selectinload(Task.assignee), selectinload(Task.reporter))
        .where(Task.id == task_id)
    )
    task = result.scalar_one_or_none()
    if task is None:
        raise ApiError(404, "Задача не найдена")
    return task


async def create_task(session: AsyncSession, data: TaskCreate, reporter: User) -> TaskView:
    if data.assignee_id is not None and await session.get(User, data.assignee_id) is None:
        raise ApiError(404, "Исполнитель не найден")
    task = Task(**data.model_dump(), reporter_id=reporter.id)
    session.add(task)
    await session.commit()
    return TaskView.model_validate(await _get_for_response(session, task.id))


async def _check_version(session: AsyncSession, task_id: UUID, version: int) -> Task:
    task = await session.get(Task, task_id)
    if task is None:
        raise ApiError(404, "Задача не найдена")
    if task.version != version:
        raise ApiError(409, "Задача изменена другим участником. Обновите доску и повторите действие.")
    return task


async def update_task(session: AsyncSession, task_id: UUID, data: TaskUpdate) -> TaskView:
    if data.assignee_id is not None and await session.get(User, data.assignee_id) is None:
        raise ApiError(404, "Исполнитель не найден")
    task = await _check_version(session, task_id, data.version)
    for field, value in data.model_dump(exclude={"version"}).items():
        setattr(task, field, value)
    try:
        await session.commit()
    except StaleDataError:
        await session.rollback()
        raise ApiError(
            409, "Задача изменена другим участником. Обновите доску и повторите действие."
        ) from None
    return TaskView.model_validate(await _get_for_response(session, task_id))


async def move_task(session: AsyncSession, task_id: UUID, data: TaskMove) -> TaskView:
    task = await _check_version(session, task_id, data.version)
    task.status = data.status
    try:
        await session.commit()
    except StaleDataError:
        await session.rollback()
        raise ApiError(409, "Задача изменена другим участником. Обновите доску.") from None
    return TaskView.model_validate(await _get_for_response(session, task_id))


async def delete_task(session: AsyncSession, task_id: UUID, version: int) -> None:
    task = await _check_version(session, task_id, version)
    await session.delete(task)
    try:
        await session.commit()
    except StaleDataError:
        await session.rollback()
        raise ApiError(409, "Задача изменена другим участником. Обновите доску.") from None
