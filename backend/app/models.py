from datetime import UTC, date, datetime
from enum import StrEnum
from uuid import UUID, uuid4

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, String, Text, Uuid
from sqlalchemy import Enum as SqlEnum
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


class Base(DeclarativeBase):
    pass


class Role(StrEnum):
    ADMIN = "admin"
    USER = "user"


class TaskStatus(StrEnum):
    TODO = "TODO"
    IN_PROGRESS = "IN_PROGRESS"
    DONE = "DONE"

    @property
    def label(self) -> str:
        return {self.TODO: "To Do", self.IN_PROGRESS: "In Progress", self.DONE: "Done"}[self]


class Priority(StrEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


role_enum = SqlEnum(
    Role,
    name="user_role",
    native_enum=False,
    values_callable=lambda roles: [r.value for r in roles],
    create_constraint=True,
    length=16,
)
status_enum = SqlEnum(
    TaskStatus,
    name="task_status",
    native_enum=False,
    values_callable=lambda statuses: [s.value for s in statuses],
    create_constraint=True,
    length=20,
)
priority_enum = SqlEnum(
    Priority,
    name="task_priority",
    native_enum=False,
    values_callable=lambda priorities: [p.value for p in priorities],
    create_constraint=True,
    length=16,
)


class User(Base):
    __tablename__ = "app_users"

    id: Mapped[UUID] = mapped_column(Uuid(as_uuid=True), primary_key=True, default=uuid4)
    name: Mapped[str] = mapped_column(String(80), nullable=False)
    email: Mapped[str] = mapped_column(String(160), nullable=False, unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[Role] = mapped_column(role_enum, nullable=False, default=Role.USER)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    assigned_tasks: Mapped[list["Task"]] = relationship(
        foreign_keys="Task.assignee_id", back_populates="assignee"
    )


class Task(Base):
    __tablename__ = "tasks"

    id: Mapped[UUID] = mapped_column(Uuid(as_uuid=True), primary_key=True, default=uuid4)
    title: Mapped[str] = mapped_column(String(160), nullable=False, index=True)
    description: Mapped[str] = mapped_column(Text, nullable=False, default="")
    status: Mapped[TaskStatus] = mapped_column(
        status_enum, nullable=False, default=TaskStatus.TODO, index=True
    )
    priority: Mapped[Priority] = mapped_column(priority_enum, nullable=False, default=Priority.MEDIUM)
    assignee_id: Mapped[UUID | None] = mapped_column(
        Uuid(as_uuid=True), ForeignKey("app_users.id", ondelete="SET NULL"), index=True
    )
    reporter_id: Mapped[UUID] = mapped_column(
        Uuid(as_uuid=True), ForeignKey("app_users.id", ondelete="RESTRICT"), nullable=False
    )
    due_date: Mapped[date | None] = mapped_column(Date)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=lambda: datetime.now(UTC), index=True
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=lambda: datetime.now(UTC),
        onupdate=lambda: datetime.now(UTC),
    )
    version: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    assignee: Mapped[User | None] = relationship(foreign_keys=[assignee_id], back_populates="assigned_tasks")
    reporter: Mapped[User] = relationship(foreign_keys=[reporter_id])
    __mapper_args__ = {"version_id_col": version}
