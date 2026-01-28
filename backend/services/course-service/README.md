# course-service

`course-service` отвечает за управление курсами и участниками курса: курсы, зачисления (роль в курсе), базовая структура курса (блоки/элементы) и настройка GitHub PAT для интеграций.

## Авторизация

- Gateway валидирует JWT и прокидывает `X-User-Id`, `X-User-Role`.
- Сервис также умеет валидировать `Authorization: Bearer ...` локально по `CODEROOM_JWT_SECRET`.

Роли:
- Глобальная роль берётся из JWT (`role`: `STUDENT|ASSISTANT|TEACHER`) и используется для грубых ограничений (например, `POST /courses` доступен только `TEACHER`).
- Роль в курсе (`RoleInCourse`: `TEACHER|ASSISTANT|STUDENT`) — источник истины здесь и используется для прав внутри курса.

## API

Все эндпоинты ниже требуют `Authorization: Bearer <accessToken>`.

### Courses

- `POST /api/v1/courses` (только глобальная роль `TEACHER`)
  - body:
    - `{ "title": "Go 101", "description": "intro", "isVisible": false }`
  - response: `CourseResponse`

- `GET /api/v1/courses`
  - список курсов, где текущий пользователь является участником
  - response: `CourseResponse[]`

- `GET /api/v1/courses/{courseId}`
  - response: `CourseResponse`

- `PUT /api/v1/courses/{courseId}` (только `RoleInCourse=TEACHER`)
  - body (все поля опциональны):
    - `{ "title": "New title", "description": "updated", "isVisible": true }`
  - response: `CourseResponse`

- `DELETE /api/v1/courses/{courseId}` (только `RoleInCourse=TEACHER`)

### Membership

- `GET /api/v1/courses/{courseId}/membership/me`
  - response:
    - `{ "courseId": "...", "userId": "...", "roleInCourse": "TEACHER" }`

### Enrollments

- `GET /api/v1/courses/{courseId}/enrollments` (любой участник курса)
  - response:
    - `{ "userId": "...", "roleInCourse": "STUDENT", "createdAt": "..." }[]`

- `POST /api/v1/courses/{courseId}/enrollments` (только `RoleInCourse=TEACHER`)
  - body:
    - `{ "userId": "...", "roleInCourse": "STUDENT" }`
  - примечание: назначение `TEACHER` этим эндпоинтом запрещено (только создатель при `POST /courses`).

- `DELETE /api/v1/courses/{courseId}/enrollments/{userId}` (только `RoleInCourse=TEACHER`)

### Course structure (blocks/items)

- `GET /api/v1/courses/{courseId}/structure` (любой участник курса)
  - response:
    - `{ "blocks": [{ "block": {...}, "items": [...] }], "rootItems": [...] }`

#### Blocks

- `POST /api/v1/courses/{courseId}/blocks` (только `RoleInCourse=TEACHER`)
  - body:
    - `{ "title": "Week 1", "position": 10, "isVisible": true }`
  - response: `BlockResponse`

- `PUT /api/v1/courses/{courseId}/blocks/{blockId}` (только `RoleInCourse=TEACHER`)
  - body:
    - `{ "title": "Week 1 (updated)", "position": 20, "isVisible": false }`
  - response: `BlockResponse`

- `DELETE /api/v1/courses/{courseId}/blocks/{blockId}` (только `RoleInCourse=TEACHER`)

#### Items

Item — это ссылка на внешний объект (материал/задание) через `refId`:
- `itemType= MATERIAL | ASSIGNMENT`
- `refId` — UUID сущности в соответствующем сервисе

- `POST /api/v1/courses/{courseId}/items` (только `RoleInCourse=TEACHER`)
  - body:
    - `{ "blockId": "...", "itemType": "MATERIAL", "refId": "...", "position": 10, "isVisible": true }`
  - response: `ItemResponse`

- `PUT /api/v1/courses/{courseId}/items/{itemId}` (только `RoleInCourse=TEACHER`)
  - body (все поля опциональны):
    - `{ "blockId": "...", "position": 20, "isVisible": false }`
  - response: `ItemResponse`

- `DELETE /api/v1/courses/{courseId}/items/{itemId}` (только `RoleInCourse=TEACHER`)

### GitHub PAT (per-course)

Токен хранится в Postgres в зашифрованном виде (AES-GCM). API не возвращает токен назад.

- `GET /api/v1/courses/{courseId}/github-pat` (только `RoleInCourse=TEACHER`)
  - response:
    - `{ "configured": true, "updatedAt": "..." }`

- `PUT /api/v1/courses/{courseId}/github-pat` (только `RoleInCourse=TEACHER`)
  - body:
    - `{ "token": "github_pat_..." }`

- `DELETE /api/v1/courses/{courseId}/github-pat` (только `RoleInCourse=TEACHER`)

## Конфигурация

### JWT

- `CODEROOM_JWT_SECRET` (shared-secret для HS256)

### DB

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### Шифрование GitHub PAT

- `CODEROOM_COURSE_GITHUB_PAT_KEY` — строка/пароль, из которого ключ шифрования детерминированно получается через SHA-256.
  - В API токен **никогда не возвращается**, только признак “задан”.
