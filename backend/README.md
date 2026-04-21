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

Файл [docker-compose.yml](docker-compose.yml) описывает **локальный стенд для разработки и демонстрации**: пароли и секреты в репозитории - **заглушки**. Для любого развёртывания «вне ноутбука» их нужно заменить и **не коммитить** реальные значения.

### Согласованность критичных секретов

- **`CODEROOM_JWT_SECRET`** должен быть **одинаковым** в `api-gateway`, `identity-service` и `course-service` (gateway валидирует JWT в Lua, сервисы - при межсервисных и прямых вызовах). Длина строки должна быть достаточной для HMAC (в шаблоне уже длинная фраза; в проде - криптостойкий случайный секрет).
- **`CODEROOM_COURSE_GITHUB_PAT_KEY`** в `course-service` - это **ключ шифрования** GitHub PAT курсов в БД (AES), а не сам PAT. Его нужно задать один раз и хранить как секрет; смена ключа без миграции данных сделает сохранённые PAT нечитаемыми.

### Инфраструктура в Compose

| Сервис | Переменные / смысл |
|--------|-------------------|
| **identity-postgres** | `POSTGRES_*` - БД `coderoom_identity`, пользователь/пароль приложения. Порт **5432** проброшен наружу для отладки. |
| **course-postgres** | Аналогично для `coderoom_course`, порт **5433**. |
| **autograding-postgres** | Аналогично для `coderoom_autograding`, порт **5434**. |
| **kafka** | KRaft-конфиг (`CLUSTER_ID`, `KAFKA_NODE_ID`, listeners). **`KAFKA_ADVERTISED_LISTENERS`** с `EXTERNAL://localhost:9094` нужен, чтобы приложения на хосте подключались к брокеру; внутри Docker-сети сервисы ходят на `kafka:9092`. |
| **minio** | `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` - root S3; API **9000**, консоль **9001**. Данные в volume `minio_data`. |

**Приватность:** в продакшене не публикуйте порты Postgres/Kafka/MinIO в интернет; используйте внутреннюю сеть, firewall и отдельные учётные записи с сильными паролями.

### Переменные приложений

- **api-gateway** - только `CODEROOM_JWT_SECRET` (совпадает с сервисами выше).
- **identity-service** - `SPRING_DATASOURCE_*`, `CODEROOM_JWT_SECRET`, при первом старте без root в БД - `CODEROOM_BOOTSTRAP_ROOT_EMAIL` / `CODEROOM_BOOTSTRAP_ROOT_PASSWORD` (в Compose можно переопределить через переменные окружения **хоста** с теми же именами). Профиль `SPRING_PROFILES_ACTIVE=docker`.
- **course-service** - БД, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `CODEROOM_JWT_SECRET`, `CODEROOM_COURSE_GITHUB_PAT_KEY`, `CODEROOM_IDENTITY_BASE_URL`, `CODEROOM_CONTENT_BASE_URL`, `CODEROOM_CONTENT_BUCKET` (имя бакета в MinIO/S3).
- **content-service** - `CODEROOM_STORAGE_INTERNAL_ENDPOINT` (для сервер-сервер, в Compose `http://minio:9000`), **`CODEROOM_STORAGE_PUBLIC_ENDPOINT`** (для presigned URL, с которого ходит **браузер**; локально `http://localhost:9000`), ключи `CODEROOM_STORAGE_ACCESS_KEY` / `SECRET_KEY`, `CODEROOM_STORAGE_BUCKET`, `CODEROOM_STORAGE_REGION`.
- **autograding-service** - только БД и Kafka (топики по умолчанию из `application.yml`).
- **runner-service** - `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `CODEROOM_CONTENT_BASE_URL`, при необходимости `CODEROOM_RUNNER_WORKSPACE_ROOT`, **`CODEROOM_RUNNER_TIMEOUT_SECONDS`** (в Compose сейчас **180**; в коде дефолт выше - для тяжёлых Java/Gradle прогонов имеет смысл увеличить в Compose или через env).

Дополнительные переменные (TTL JWT, имена Kafka-топиков, `CODEROOM_GITHUB_*`, время жизни presigned URL и т.д.) описаны в `application.yml` соответствующих модулей и в README:

- [services/identity-service/README.md](services/identity-service/README.md) - JWT TTL, bootstrap root.
- [services/course-service/README.md](services/course-service/README.md) - GitHub API/clone URL, Kafka-топики, PAT encryption key, URL identity/content.
- [services/content-service/README.md](services/content-service/README.md) - TTL presign, S3-поля.
- [services/autograding-service/README.md](services/autograding-service/README.md) и [services/runner-service/README.md](services/runner-service/README.md) - топики и таймауты раннера.

### Рекомендации по приватности секретов

1. Храните прод-значения в **секрет-хранилище** (Docker secrets, Vault, переменные CI/CD) или в **`.env` рядом с compose**, добавив `.env` в `.gitignore`.
2. Не переиспользуйте dev-пароли БД и MinIO в общедоступных средах.
3. Регулярно меняйте пароль root после первого входа (или задайте сильный `CODEROOM_BOOTSTRAP_ROOT_PASSWORD` до первого деплоя и не светите его в логах).
4. Для HTTPS выносите **только** `api-gateway` за reverse proxy (nginx/Caddy); остальные сервисы оставьте во внутренней сети без публикации портов наружу.

### Рекомендации оператору

- **Локальная демонстрация курса:** достаточно `docker compose up --build` из `backend/`, затем создать root по env, выдать роль преподавателя, настроить курс и при необходимости GitHub PAT в UI - как в основном README выше.
- **Стенд для группы студентов:** тот же compose на ВМ в закрытой сети; смените все секреты и `CODEROOM_STORAGE_PUBLIC_ENDPOINT` на URL MinIO/S3, **доступный браузеру студентов** (тот же хост/порт, по которому они открывают LMS), иначе presigned-загрузки/скачивания с другого хоста не сработают.
- **Резервное копирование:** тома `*_postgres_data`, `kafka_data`, `minio_data` - единственное долговременное состояние стенда; планируйте snapshot или вынос БД и объектного хранилища на управляемые сервисы при росте требований.
- **Масштабирование:** `runner-service` и при необходимости `autograding-service` можно поднять в нескольких экземплярах при одной Kafka и общей БД autograding (с учётом consumer group); детали - в README этих сервисов.
