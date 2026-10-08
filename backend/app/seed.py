"""Create the configured administrator and local demo data idempotently."""

import asyncio

from sqlalchemy import select

from app.core.config import get_settings
from app.core.security import hash_password
from app.database import SessionFactory, engine
from app.models import Priority, Role, Task, TaskStatus, User


async def seed() -> None:
    settings = get_settings()
    if not settings.admin_email or not settings.admin_password.get_secret_value():
        if settings.app_env.lower() == "production":
            raise RuntimeError("ADMIN_EMAIL and ADMIN_PASSWORD must be set")
        admin_email = "admin@example.com"
        admin_password = "Admin-demo-2026!"
    else:
        admin_email = settings.admin_email
        admin_password = settings.admin_password.get_secret_value()

    async with SessionFactory() as session, session.begin():
        admin = await session.scalar(select(User).where(User.email == admin_email.lower()))
        if admin is None:
            admin = User(
                name="Администратор",
                email=admin_email.lower(),
                password_hash=hash_password(admin_password),
                role=Role.ADMIN,
            )
            session.add(admin)
            await session.flush()

        if settings.app_env.lower() == "dev":
            demo_users = [
                ("Алина Ким", "alina@example.com", "User-demo-2026!"),
                ("Данил Соколов", "danil@example.com", "User-demo-2026!"),
            ]
            for name, email, password in demo_users:
                user = await session.scalar(select(User).where(User.email == email))
                if user is None:
                    user = User(name=name, email=email, password_hash=hash_password(password), role=Role.USER)
                    session.add(user)
                    await session.flush()
            count = await session.scalar(select(Task.id).limit(1))
            if count is None:
                people = list((await session.scalars(select(User).order_by(User.email))).all())
                for title, description, status, priority, assignee in [
                    (
                        "Собрать обратную связь команды",
                        "Провести короткие интервью и оформить выводы.",
                        TaskStatus.IN_PROGRESS,
                        Priority.HIGH,
                        "alina@example.com",
                    ),
                    (
                        "Подготовить план релиза",
                        "Согласовать ключевые этапы и владельцев.",
                        TaskStatus.TODO,
                        Priority.MEDIUM,
                        "danil@example.com",
                    ),
                    (
                        "Настроить рабочее пространство",
                        "Создать базовые процессы команды.",
                        TaskStatus.DONE,
                        Priority.LOW,
                        "admin@example.com",
                    ),
                ]:
                    assignee_user = next((person for person in people if person.email == assignee), None)
                    session.add(
                        Task(
                            title=title,
                            description=description,
                            status=status,
                            priority=priority,
                            reporter_id=admin.id,
                            assignee_id=assignee_user.id if assignee_user else None,
                        )
                    )


async def main() -> None:
    await seed()
    await engine.dispose()


if __name__ == "__main__":
    asyncio.run(main())
