# Teamflow — Mini-Jira для команды

Full-stack Kanban-приложение: **Java 17 / Spring Boot 3.5.16**, **Angular 22.2**, **PostgreSQL 17**. Реальные API и хранение данных, без подмены сервера браузерными моками.

## Возможности

- Общая доска команды: **To Do → In Progress → Done**, drag-and-drop Angular CDK и доступный с клавиатуры выбор статуса.
- Создание и редактирование задач, описание, приоритет, исполнитель, срок; поиск и фильтры.
- Пользователи `USER` и `ADMIN`. Администратор создаёт аккаунты и удаляет задачи; обе роли работают с задачами общей команды.
- Явный Java **Builder**, DTO, Bean Validation, сервисные транзакции и репозитории.
- Spring Security OAuth2 Resource Server проверяет JWT: подпись HS256, issuer, audience, срок действия. BCrypt для паролей.
- `@RestControllerAdvice`, единый JSON ошибок, `X-Request-ID` и логи действий без паролей и токенов.
- Оптимистическая блокировка JPA `@Version`: устаревшие изменения и удаления получают **409**.
- Flyway, индексы, PostgreSQL; отдельный dev-профиль с файловой H2 и демонстрационными данными.
- Lazy routes для авторизации и доски, standalone-компоненты, signals, переиспользуемые модальные окна с focus trap и уведомления.
- Docker Compose, Maven Wrapper, lockfile pnpm, CI для Java, Angular и Playwright.

## Быстрый локальный запуск без Docker

Нужны **JDK 17–25**, **Node.js 24.19+**, **pnpm 11.25.0**. Maven скачает wrapper. Системная Java 8 не подходит: задайте `JAVA_HOME` на установленный JDK 17. Совместимость: [Spring Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Angular](https://angular.dev/reference/versions).

Первый терминал, из папки проекта:

```powershell
cd backend
# При необходимости: $env:JAVA_HOME = 'C:\path\to\jdk-17'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
```

Второй терминал:

```powershell
cd frontend
pnpm install --frozen-lockfile
pnpm start
```

Для Linux/macOS: `sh mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.

Откройте **http://127.0.0.1:4202**. API: `http://127.0.0.1:8082`. Angular проксирует `/api` на бэкенд, поэтому запросы идут в один origin.

Только в профиле `dev` автоматически создаются:

| Роль | Email | Пароль |
|---|---|---|
| ADMIN | `admin@teamflow.local` | `Admin-demo-2026!` |
| USER | `alina@teamflow.local` | `User-demo-2026!` |
| USER | `danil@teamflow.local` | `User-demo-2026!` |

Dev-данные сохраняются в `backend/data`, исключённой из Git. Повторный запуск не сбрасывает ваши задачи. Токен хранится **только в памяти**: после перезагрузки вкладки потребуется войти заново. Через 30 минут сессия завершается.

## PostgreSQL и Docker Compose

```powershell
Copy-Item .env.example .env
# Замените ВСЕ CHANGE_ME в .env собственными значениями.
docker compose up --build -d
```

Открывается тот же адрес `http://127.0.0.1:4202`. Остановите локальный Angular перед запуском Compose. PostgreSQL и API доступны только внутри сети Compose, наружу опубликован Nginx на loopback. Здесь dev-профиль **не включён**: создаётся только администратор из `.env`, без демонстрационных задач и известных паролей.

JWT_SECRET: случайная строка не менее 32 байт; можно сгенерировать через `node -e "console.log(require('crypto').randomBytes(48).toString('base64'))"`. Пароль администратора — 12–64 символа. `.env` не коммитится. Bootstrap создаёт администратора, только если такого email ещё нет; смена переменной не меняет пароль существующего аккаунта.

Перед внешней эксплуатацией нужны HTTPS на ingress, резервное копирование PostgreSQL, хранение секретов вне репозитория и проверка эксплуатационных требований вашей организации. Ограничение попыток входа включено на Nginx. В прямом dev API rate limit отсутствует.

## Структура

```text
mini-jira/
├── backend/
│   ├── src/main/java/com/teamflow/
│   │   ├── auth/       # login, JWT encoder/decoder, SecurityFilterChain
│   │   ├── common/     # ControllerAdvice, ApiError, request logging
│   │   ├── config/     # bootstrap и dev-данные
│   │   ├── task/       # Task + Builder, DTO, controller/service/repository
│   │   └── user/       # пользователи и роли
│   ├── src/main/resources/db/migration/
│   ├── src/test/java/com/teamflow/
│   └── mvnw, mvnw.cmd
├── frontend/src/app/
│   ├── core/          # типы, API-сервис, auth guard/interceptor
│   ├── shared/        # modal, alert, icon
│   └── features/
│       ├── auth/      # lazy routes + login
│       └── board/     # lazy routes + Kanban + формы
├── frontend/e2e/       # браузерные тесты против реального API
├── .github/workflows/ci.yml
└── compose.yml
```

## Builder на Java

Полная реализация: [`Task.java`](backend/src/main/java/com/teamflow/task/Task.java). Builder принимает обязательные поля в конструктор, задаёт defaults и проверяет инварианты при `build()`. Сущность имеет защищённый no-arg конструктор только для JPA и не предоставляет произвольные сеттеры.

```java
Task task = Task.builder("Реализовать авторизацию", reporter)
    .description("JWT, роли и защищённые REST endpoints")
    .priority(Priority.HIGH)
    .assignee(developer)
    .dueDate(LocalDate.now().plusDays(7))
    .build(); // TODO по умолчанию; независимый UUID и timestamps
```

Сокращённая структура паттерна:

```java
public static Builder builder(String title, AppUser reporter) {
    return new Builder(title, reporter);
}

public static final class Builder {
    private final String title;
    private final AppUser reporter;
    private Priority priority = Priority.MEDIUM;
    // Другие необязательные поля...

    private Builder(String title, AppUser reporter) {
        this.title = title;
        this.reporter = reporter;
    }
    public Builder priority(Priority value) {
        priority = value;
        return this;
    }
    public Task build() { return new Task(this); }
}
```

## Защищённый endpoint Spring Security

Настройка: [`SecurityConfig.java`](backend/src/main/java/com/teamflow/auth/SecurityConfig.java). `roles` из подписанного JWT преобразуются в `ROLE_ADMIN` / `ROLE_USER`. `@EnableMethodSecurity` включает проверку `@PreAuthorize`.

```java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
@PreAuthorize("hasRole('ADMIN')")
public void delete(@PathVariable UUID id,
                   @RequestParam @Min(0) long version,
                   @AuthenticationPrincipal Jwt jwt) {
    tasks.delete(id, version, UUID.fromString(jwt.getSubject()));
}
```

Запрос без токена получает **401**, от USER — **403**, со старой версией — **409**. UI скрывает удаление у USER, но защита действует на сервере независимо от интерфейса. Reporter берётся из JWT, а не из JSON клиента.

## Kanban и Lazy Loading на Angular

[`app.routes.ts`](frontend/src/app/app.routes.ts):

```typescript
export const routes: Routes = [
  { path: 'auth', loadChildren: () => import('./features/auth/auth.routes')
      .then(m => m.AUTH_ROUTES) },
  { path: 'board', canActivate: [authGuard],
    loadChildren: () => import('./features/board/board.routes')
      .then(m => m.BOARD_ROUTES) }
];
```

`board.routes.ts` дополнительно загружает `BoardComponent` через `loadComponent`. Это современное разделение feature-разделов на standalone route trees, без обязательных `NgModule`.

Структура [`BoardComponent`](frontend/src/app/features/board/board.component.ts):

```typescript
@Component({
  imports: [DragDropModule, FormsModule, TaskFormComponent, /* ... */],
  templateUrl: './board.component.html'
})
export class BoardComponent {
  readonly tasks = signal<Task[]>([]);
  readonly columns = signal<Column[]>([]);
  readonly pending = signal(false);

  drop(event: CdkDragDrop<TaskStatus>) {
    const task: Task = event.item.data;
    this.move(task, event.container.data);
  }
  // move вызывает PATCH /api/tasks/{id}/status с status и version.
  // Обновление локального массива — только после успешного ответа API.
}
```

Шаблон использует `cdkDropListGroup`, `cdkDropList` и `cdkDrag`. Сортировка внутри колонок отключена: сервер хранит статус, а порядок карточек определяется временем создания. Пока перенос сохраняется, повторное перемещение блокируется. При 409 доска перечитывается. При конфликте редактирования текст в форме сохраняется для ручного переноса.

## REST API

Все endpoints, кроме login и health, требуют `Authorization: Bearer <token>`.

| Метод и путь | Доступ | Назначение |
|---|---|---|
| `POST /api/auth/login` | Публичный | Email + password → JWT, expiresAt, user |
| `GET /api/auth/me` | USER, ADMIN | Текущий участник |
| `GET /api/users` | USER, ADMIN | Участники команды |
| `POST /api/users` | ADMIN | Создать участника |
| `GET /api/statuses` | USER, ADMIN | Три фиксированных статуса |
| `GET /api/tasks?page=0&size=100` | USER, ADMIN | Страница задач, size 1–100 |
| `POST /api/tasks` | USER, ADMIN | Создать задачу в TODO |
| `PUT /api/tasks/{id}` | USER, ADMIN | Изменить поля с проверкой version |
| `PATCH /api/tasks/{id}/status` | USER, ADMIN | Переместить задачу с проверкой version |
| `DELETE /api/tasks/{id}?version=0` | ADMIN | Удалить задачу с проверкой version |
| `GET /actuator/health` | Публичный | Health без внутренних подробностей |

Создание задачи:

```json
{"title":"Подготовить релиз","description":"Проверить сценарии","priority":"HIGH","assigneeId":null,"dueDate":"2027-01-15"}
```

Перемещение:

```json
{"status":"IN_PROGRESS","version":0}
```

Ответ ошибки:

```json
{"timestamp":"2026-10-03T07:00:00Z","status":409,"message":"Задача уже изменена. Обновите доску и повторите действие.","requestId":"uuid","fields":{}}
```

Неизвестные JSON-поля отклоняются. Сущности JPA и хеши паролей не возвращаются клиенту. Текущий Kanban получает все страницы API и фильтрует их локально — это осознанный предел для небольшой команды, а не бесконечная доска на миллионы задач.

## Проверки

```powershell
cd backend
.\mvnw.cmd verify
cd ../frontend
pnpm build
pnpm exec playwright install chromium
pnpm test:e2e
```

Если на Windows уже установлен Edge, можно использовать его без скачивания Chromium:

```powershell
$env:PLAYWRIGHT_CHANNEL = 'msedge'
pnpm test:e2e
```

Playwright сам поднимает dev API и Angular или использует уже работающие локальные серверы. Эти тесты предназначены для локальной/dev-базы, не для рабочего окружения. После успешного CRUD-сценария тестовая задача удаляется. В случае прерывания может остаться задача с префиксом `E2E task`.

Java: Builder, вход, невалидные/просроченные токены, audience, права, переходы статусов, создание пользователей, валидация, пагинация, optimistic locking. Один API-контракт запускается на H2 и на PostgreSQL Testcontainers. PostgreSQL-тесты автоматически пропускаются без Docker; CI на Ubuntu запускает их с Docker.

CI предполагает, что содержимое `mini-jira` станет корнем отдельного репозитория. Пока папка находится внутри другого репозитория, вложенный workflow не запускается GitHub автоматически.

## Границы проекта

Это реализация Mini-Jira для **одной общей команды** с enterprise-приёмами, а не полноценная multi-tenant Jira. Публичной регистрации нет: доступ выдаёт ADMIN. Все допущенные участники видят общую доску и могут редактировать её задачи. Пользовательские колонки, realtime/WebSocket, комментарии, вложения, SSO, refresh tokens, немедленный отзыв JWT, восстановление паролей и долговременный audit storage не включены. Выход очищает токен в браузере; уже выданный JWT остаётся криптографически действительным до истечения срока. Логи действий не заменяют отдельный неизменяемый журнал аудита.

Справочные материалы: [JWT в Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), [Angular CDK Drag and Drop](https://angular.dev/guide/drag-drop), [настройки сборки pnpm 11](https://pnpm.io/blog/releases/11.0).

Git commit и push не выполнялись. Файлы подготовлены для отдельного репозитория; даты Git не подменялись.
