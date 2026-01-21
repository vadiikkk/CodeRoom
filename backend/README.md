# CodeRoom backend

Backend реализуется как монорепо с несколькими микросервисами в `backend/services/*`.

## Локальный запуск (dev)

Требования: Docker / Docker Compose.

Из папки `backend/`:

- поднять инфраструктуру + `identity-service`:
  - `docker compose up --build`

После запуска:
- `identity-service`: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- OpenAPI json: `http://localhost:8081/v3/api-docs`
- Postgres (identity): `localhost:5432` (db: `coderoom_identity`, user/pass: `identity_service`)
- Kafka (dev, external listener): `localhost:9094`

## Микросервисы

- `services/identity-service`: регистрация/логин + JWT
