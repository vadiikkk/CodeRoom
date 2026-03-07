# autograding-service

Kafka-оркестратор автопроверки CodeRoom: получает запрос на проверку, создает `test_runs`, отправляет job в `runner-service` и публикует нормализованный результат обратно в `course-service`.

## Важные правила

- Сервис не имеет публичного API для фронта; рабочий контур идет через Kafka.
- Источник истины по итоговой оценке и видимому состоянию attempt - `course-service`.
- При retry используется тот же `attemptId`: `autograding-service` переиспользует существующий `test_run`, очищает прошлый результат и ставит его в очередь заново.

## Kafka flow

- `AutogradeRequested` из `course-service`
- `RunTestRun` в `runner-service`
- `RunnerFinished` из `runner-service`
- `AutogradeFinished` обратно в `course-service`

## Kafka topics

- `coderoom.autograde.jobs`
- `coderoom.runner.jobs`
- `coderoom.runner.results`
- `coderoom.autograde.results`

## Хранилище

Сервис хранит `test_runs` в Postgres:

- связь `attemptId -> testRunId`
- snapshot `.coderoom.yml`
- статус, score, summary, ссылки на логи
- diagnostic fields: `exitCode`, `testsPassed`, `testsTotal`, `scoringMode`

## Конфигурация

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `CODEROOM_KAFKA_TOPIC_AUTOGRADE_JOBS`
- `CODEROOM_KAFKA_TOPIC_RUNNER_JOBS`
- `CODEROOM_KAFKA_TOPIC_RUNNER_RESULTS`
- `CODEROOM_KAFKA_TOPIC_AUTOGRADE_RESULTS`
