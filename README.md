# Teamflow — Mini-Jira для команды

Full-stack Kanban для небольшой команды: **FastAPI / Python**, **Angular**, **PostgreSQL** в production и SQLite для разработки. API хранит задачи и аккаунты в базе, проверяет роли `admin` / `user`, выдаёт JWT через OAuth2 Password flow и возвращает единый формат ошибок.

## Что умеет приложение

- Вход команды по аккаунтам, которые создаёт администратор; открытой регистрации нет.
- Просмотр задач, фильтр по названию, исполнителю и приоритету; поиск всех страниц API.
- Создание, редактирование и назначение задач, сроков и приоритетов.
- Перемещение карточек между колонками `To Do`, `In Progress` и `Done` перетаскиванием или выбором статуса.
- Управление участниками для администратора и удаление задач с проверкой роли на сервере.
- Проверка версии задачи предотвращает перезапись параллельных изменений.
- Ленивая загрузка экранов авторизации и доски, переиспользуемые модальные окна, уведомления и JWT interceptor.
- Alembic миграции, Docker Compose, структурированные логи, request ID и CI.

## Быстрый запуск на Windows

Нужны Python 3.12–3.14, Node.js 24 и pnpm 11. Для локальной разработки сервер использует SQLite.

```powershell
cd backend
py -3.14 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python -m alembic upgrade head
python -m app.seed
python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8082
```

В другом терминале:

```powershell
cd frontend
pnpm install --frozen-lockfile
pnpm start
```

Откройте `http://127.0.0.1:4202`. В локальном демо создаются:

| Роль | Email | Пароль |
|---|---|---|
| admin | `admin@example.com` | `Admin-demo-2026!` |
| user | `alina@example.com` | `User-demo-2026!` |
| user | `danil@example.com` | `User-demo-2026!` |

Демо-аккаунты предназначены только для локального запуска. API docs доступны на `http://127.0.0.1:8082/docs` в режиме `APP_ENV=dev`.

Для тестов браузера сначала установите зависимости в `backend\.venv` и браузер `pnpm exec playwright install chromium` из `frontend`, затем выполните `pnpm test:e2e`. Playwright самостоятельно поднимет API и Angular.

## Production запуск

Скопируйте `.env.example` в `.env`, задайте `APP_ENV=production`, надёжный `JWT_SECRET` (не менее 32 байт), `ADMIN_EMAIL`, `ADMIN_PASSWORD` (не менее 12 символов) и случайный `DB_PASSWORD`. `DATABASE_URL` для Compose собирается из `DB_PASSWORD`. Затем выполните `docker compose up --build -d`. Приложение будет доступно через `http://localhost:4202`; frontend-контейнер проксирует API и ограничивает частоту входа. При первом запуске создаются схема и единственная административная учётная запись из переменных окружения. Не используйте демо-пароли в production.

## Структура

```text
backend/
  app/
    core/          # конфигурация, JWT, зависимости ролей, логирование
    crud/          # операции SQLAlchemy и транзакции
    routers/       # auth, users, tasks/statuses
    models.py      # SQLAlchemy модели и enum
    schemas.py     # Pydantic входные/выходные контракты
    exceptions.py  # доменная ошибка
    main.py        # FastAPI, CORS, обработчики и request ID
  migrations/      # Alembic
  tests/           # API и проверки доступа
frontend/src/app/
  core/            # API, модели, auth guard/interceptor
  features/auth/   # лениво загружаемый вход
  features/board/  # Kanban, задачи, участники
  shared/          # модальные окна и уведомления
```

## JWT OAuth2 в FastAPI

Пароль хешируется Argon2, токен подписан HS256 и проверяется по подписи, сроку, issuer и audience. Роль берётся из базы после проверки токена, а не из данных запроса. OAuth2 парольный flow принимает `application/x-www-form-urlencoded` (`username` содержит email):

```python
@router.post("/login", response_model=OAuthSession)
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
        raise HTTPException(status_code=401, detail="Неверный email или пароль")
    token, expires_in = create_access_token(user, settings)
    return {"access_token": token, "token_type": "bearer", "expires_in": expires_in, "user": user}
```

Защищённый административный маршрут использует общую зависимость проверки роли:

```python
@router.delete("/tasks/{task_id}", status_code=204)
async def delete_task(
    task_id: UUID,
    session: DbSession,
    _: Annotated[User, Depends(require_role(Role.ADMIN))],
    version: int = Query(ge=0),
) -> Response:
    await tasks_crud.delete_task(session, task_id, version)
    return Response(status_code=204)
```

Неавторизованный запрос получает `401`, пользователь без административной роли — `403`. Изменения задачи требуют актуальную `version`; конфликт возвращает `409`.

## Angular Kanban и interceptor

Экран доски загружается маршрутом через `loadChildren`. CDK передаёт статус целевой колонки, API обновляет задачу с её текущей версией:

```ts
drop(event: CdkDragDrop<TaskStatus>) {
  const task: Task = event.item.data;
  this.move(task, event.container.data);
}

move(task: Task, status: TaskStatus) {
  if (task.status === status || this.pending()) return;
  this.pending.set(true);
  this.api.move(task, status)
    .pipe(finalize(() => this.pending.set(false)))
    .subscribe({
      next: updated => this.tasks.update(items => items.map(item => item.id === task.id ? updated : item)),
      error: error => this.alerts.error(error),
    });
}
```

Сам список колонок использует `cdkDropListGroup`, а каждая карточка — `cdkDrag`. `authInterceptor` добавляет `Authorization: Bearer …` ко всем API-запросам; вход отправляет OAuth2 форму и хранит сессию в памяти браузера. После перезагрузки страницы требуется войти снова.

## REST API

| Метод и путь | Доступ | Назначение |
|---|---|---|
| `POST /api/auth/login` | публичный | OAuth2 форма, выдача JWT |
| `GET /api/auth/me` | user/admin | Текущий пользователь |
| `GET /api/users` | user/admin | Участники команды |
| `POST /api/users` | admin | Создать участника |
| `GET /api/statuses` | user/admin | Три фиксированные колонки |
| `GET /api/tasks?page=0&size=100` | user/admin | Пагинированный список задач |
| `POST /api/tasks` | user/admin | Создать задачу |
| `PUT /api/tasks/{id}` | user/admin | Изменить задачу с `version` |
| `PATCH /api/tasks/{id}/status` | user/admin | Переместить задачу с `version` |
| `DELETE /api/tasks/{id}?version=0` | admin | Удалить задачу с `version` |

## Проверки и границы

```powershell
cd backend
.\.venv\Scripts\Activate.ps1
ruff check app tests migrations
pytest --cov=app --cov-report=term-missing
cd ..\frontend
pnpm build
pnpm test:e2e
```

Тесты покрывают OAuth2-вход, роли, формат ошибок, создание/перемещение/удаление и конкурентное изменение задач. Это общая доска одной команды: workspace isolation, публичная регистрация, refresh tokens, отзыв уже выданного JWT, email-сброс пароля, комментарии, вложения и realtime-обновления не реализованы. JWT хранится только в памяти браузера.

Подробности OAuth2/JWT: [FastAPI Security](https://fastapi.tiangolo.com/tutorial/security/oauth2-jwt/); перетаскивание карточек: [Angular CDK Drag and Drop](https://angular.dev/guide/drag-drop).
