# CodeRoom backend

Backend реализуется как монорепо с пятью микросервисами на Kotlin в backend/services/* и api-gateway (Nginx + Lua).

## Локальный запуск

Из директории backend/:

- `docker compose up --build`

После запуска доступны:

- api-gateway: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI `identity-service`: `http://localhost:8080/identity/v3/api-docs`
- OpenAPI `course-service`: `http://localhost:8080/course/v3/api-docs`
- Postgres `identity-service`: `localhost:5432`
- Postgres `course-service`: `localhost:5433`
- Postgres `autograding-service`: `localhost:5434`
- Kafka external listener: `localhost:9094`
- MinIO API: `http://localhost:9000`
- MinIO console: `http://localhost:9001` (`minio` / `minio12345`)

Локально `content-service` генерирует presigned URL через:

- internal endpoint: `http://minio:9000`
- public endpoint: `http://localhost:9000`

## Микросервисы

- `api-gateway`: Единая точка входа: маршрутизация, CORS, rate limit, валидация JWT на входе и прокидывание `X-User-Id`, `X-User-Role` в сервисы, агрегированный Swagger.
- `services/identity-service`: регистрация, логин, JWT, глобальные роли, root-admin, lookup пользователей.
- `services/course-service`: LMS-домен, курсы, материалы, задания, интеграция с GitHub, сдача заданий, проверка и журнал.
- `services/content-service`: internal presign/download/delete для MinIO/S3.
- `services/autograding-service`: Kafka-оркестратор автопроверки, который создает тестовые прогоны, нормализует результаты и отправляет итог обратно в `course-service`.
- `services/runner-service`: worker, который забирает job из Kafka, клонирует PR, подмешивает приватные тесты, запускает команду из `.coderoom.yml` и публикует результат.

## Документация по настройке и развертыванию

`TBD`
