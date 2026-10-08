from datetime import date, datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator
from pydantic.alias_generators import to_camel

from app.models import Priority, Role, TaskStatus


class StrictSchema(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="forbid",
        str_strip_whitespace=True,
    )


class UserView(StrictSchema):
    model_config = ConfigDict(from_attributes=True, alias_generator=to_camel, populate_by_name=True)
    id: UUID
    name: str
    email: EmailStr
    role: Role
    is_active: bool


class CreateUser(StrictSchema):
    name: str = Field(min_length=1, max_length=80)
    email: EmailStr
    password: str = Field(min_length=12, max_length=64)
    role: Role = Role.USER


class TaskCreate(StrictSchema):
    title: str = Field(min_length=1, max_length=160)
    description: str = Field(default="", max_length=4000)
    priority: Priority = Priority.MEDIUM
    assignee_id: UUID | None = None
    due_date: date | None = None

    @field_validator("title")
    @classmethod
    def title_must_contain_text(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("Название задачи не может быть пустым")
        return value.strip()


class TaskUpdate(TaskCreate):
    version: int = Field(ge=0)


class TaskMove(StrictSchema):
    status: TaskStatus
    version: int = Field(ge=0)


class TaskView(StrictSchema):
    model_config = ConfigDict(from_attributes=True, alias_generator=to_camel, populate_by_name=True)
    id: UUID
    title: str
    description: str
    status: TaskStatus
    priority: Priority
    assignee: UserView | None
    reporter: UserView
    due_date: date | None
    created_at: datetime
    updated_at: datetime
    version: int


class TaskPage(StrictSchema):
    content: list[TaskView]
    total_elements: int
    total_pages: int
    number: int


class ColumnView(StrictSchema):
    id: TaskStatus
    label: str


class OAuthSession(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    access_token: str
    token_type: str = "bearer"
    expires_in: int
    user: UserView
