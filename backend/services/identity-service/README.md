# identity-service

Сервис аутентификации/авторизации для CodeRoom: регистрация/логин, JWT access/refresh, роли пользователей и root-пользователь для администрирования.

## Роли и root-пользователь

- **Регистрация всегда создаёт пользователя с ролью `STUDENT`**.
- Выдавать роли `ASSISTANT` / `TEACHER` может только **root-пользователь**.
- Root — единственный в системе (`users.is_root = true` может быть только у одного пользователя).
- Пользователь может быть деактивирован (`is_active=false`) — логин/refresh будут недоступны.

### Bootstrap root при старте

Если root ещё не создан, сервис требует указать креды root (иначе старт завершится ошибкой).

Env-переменные (обычно задаются в `backend/docker-compose.yml`/`.env`):

- `CODEROOM_BOOTSTRAP_ROOT_EMAIL`
- `CODEROOM_BOOTSTRAP_ROOT_PASSWORD`

## TL;DR

- **Base URL (dev)**: `http://localhost:8080` (через `api-gateway`)
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## API

### Auth

- `POST /api/v1/auth/register`
  - body: `{ "email": "student@example.com", "password": "secret1234" }`
  - response: `{ "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer" }`
- `POST /api/v1/auth/login`
  - body: `{ "email": "student@example.com", "password": "secret1234" }`
  - response: `{ "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer" }`
- `POST /api/v1/auth/refresh`
  - body: `{ "refreshToken": "..." }`
  - response: `{ "accessToken": "...", "refreshToken": "...", "tokenType": "Bearer" }`
- `POST /api/v1/auth/logout`
  - body: `{ "refreshToken": "..." }`
  - revokes этот refresh token (idempotent)
- `POST /api/v1/auth/logout-all`
  - body: `{ "refreshToken": "..." }`
  - revokes все refresh токены пользователя, которому принадлежит переданный refresh token

### Me

- `GET /api/v1/me`
  - response: `{ "userId": "...", "email": "...", "role": "STUDENT" }`
- `POST /api/v1/me/password`
  - headers: `Authorization: Bearer <accessToken>`
  - body: `{ "oldPassword": "...", "newPassword": "..." }`
  - меняет пароль и ревокает все refresh токены пользователя

### Admin (только root)

- `GET /api/v1/admin/users?q=...&page=0&size=50`
  - headers: `Authorization: Bearer <accessToken-root>`
  - список пользователей (поиск по email через `q`, пагинация)
- `PUT /api/v1/admin/users/role`
  - headers: `Authorization: Bearer <accessToken-root>`
  - body: `{ "email": "user@example.com", "role": "TEACHER" }`
  - Назначает роль пользователю по email (email нормализуется в lower-case).
- `PUT /api/v1/admin/users/active`
  - headers: `Authorization: Bearer <accessToken-root>`
  - body: `{ "email": "user@example.com", "isActive": false }`
  - деактивация/активация пользователя; при деактивации ревокает все refresh токены пользователя

## Конфигурация

### JWT

- `CODEROOM_JWT_SECRET` (обязательно; для HMAC нужна строка достаточной длины)
- `CODEROOM_JWT_ACCESS_TTL_SECONDS` (default: `3600`)
- `CODEROOM_JWT_REFRESH_TTL_SECONDS` (default: `2592000`)

### DB

Подключение стандартными переменными Spring:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Локальная разработка

Сервис собирается из корня `backend/`:

```bash
./gradlew :services:identity-service:build
```
