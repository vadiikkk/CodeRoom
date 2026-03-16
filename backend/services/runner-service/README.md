# runner-service

Worker автопроверки CodeRoom: исполняет check-команду в изолированном рабочем каталоге, собирает лог и возвращает нормализованный результат в Kafka.

## Важные правила

- Публичного API нет; сервис работает только через Kafka jobs/results.
- Поддерживаются `GO`, `PYTHON`, `JAVA`.
- Расширение идет через `LanguageExecutor`, orchestration слой остается общим.
- Private tests скачиваются через internal object transfer `content-service`, а не через public presigned URL.

## Runner flow

1. Получает `RunTestRun` из Kafka.
2. Клонирует assignment repository и делает `git fetch origin pull/<number>/head`.
3. Checkout-ит PR head.
4. Накладывает private tests в `privateTests.targetPath`, если они включены.
5. Запускает `check.command` в `check.workdir`.
6. Загружает runner log в MinIO.
7. Публикует `RunnerFinished` в Kafka.

## Scoring

- Для `ALL_OR_NOTHING` итог зависит от `exitCode`.
- Для `PASS_RATE` сервис пытается извлечь число пройденных тестов:
  - Go: из `go test -json`
  - Python/Java: из JUnit XML
- В `RunnerFinished` прокидываются `score`, `exitCode`, `testsPassed`, `testsTotal`, `scoringMode`, `comment`, `resultSummary`, `logObjectKey`.

Дефолтный `privateTests.targetPath` зависит от языка:

- `GO` -> `.`
- `PYTHON` -> `tests`
- `JAVA` -> `src/test/java`

## Конфигурация

- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `CODEROOM_KAFKA_TOPIC_RUNNER_JOBS`
- `CODEROOM_KAFKA_TOPIC_RUNNER_RESULTS`
- `CODEROOM_CONTENT_BASE_URL`
- `CODEROOM_RUNNER_WORKSPACE_ROOT`
- `CODEROOM_RUNNER_TIMEOUT_SECONDS`
