# CodeRoom backend

Backend реализуется как монорепо с несколькими микросервисами в `backend/services/*`.

## Локальный запуск (dev)

Требования: Docker / Docker Compose.

Из папки `backend/`:

- поднять инфраструктуру + `identity-service` + `api-gateway`:
  - `docker compose up --build`

После запуска:
- `api-gateway` (единая точка входа): `http://localhost:8080`
- Swagger UI (единый, агрегирует несколько сервисов): `http://localhost:8080/swagger-ui/index.html`
- OpenAPI json (identity-service через gateway): `http://localhost:8080/v3/api-docs`
- OpenAPI json (course-service через gateway): `http://localhost:8080/course/v3/api-docs`
- Postgres (identity): `localhost:5432` (db: `coderoom_identity`, user/pass: `identity_service`)
- Kafka (dev, external listener): `localhost:9094`

## Микросервисы

- `services/identity-service`: регистрация/логин + JWT
- `services/course-service`: курсы, участники курса, базовая структура курса
