# course-service

LMS-сервис CodeRoom: курсы, структура, материалы, задания, группы, сдачи заданий, журнал, GitHub PAT и оркестрация кодовых заданий.

## Важные правила

- Все публичные ручки требуют `Authorization: Bearer <accessToken>`.
- Gateway валидирует JWT и прокидывает `X-User-Id`, `X-User-Role`; сервис также умеет локально валидировать токен по `CODEROOM_JWT_SECRET`.
- Права определяются двумя уровнями: глобальная роль `STUDENT|ASSISTANT|TEACHER` и роль в курсе `RoleInCourse: TEACHER|ASSISTANT|STUDENT`.
- Для студентов скрытый курс или скрытый контент ведут себя как `not found`.
- `assignment.weight` обязателен и находится в диапазоне `[0,1]`.
- Для `CODE` заданий новая PR-ссылка создает новую попытку.

## API

Все публичные ручки:

- headers: `Authorization: Bearer <accessToken>`

### Courses

- `POST /api/v1/courses`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "title": "Algorithms", "description": "Spring semester", "isVisible": false }`
  - response: `CourseResponse`
- `GET /api/v1/courses`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `[CourseResponse]`
- `GET /api/v1/courses/{courseId}`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `CourseResponse`
- `PUT /api/v1/courses/{courseId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "title": "Updated title", "description": "Updated description", "isVisible": true }`
  - response: `CourseResponse`
- `DELETE /api/v1/courses/{courseId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`

### Membership and Enrollments

- `GET /api/v1/courses/{courseId}/membership/me`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `MyMembershipResponse`
- `GET /api/v1/courses/{courseId}/enrollments`
  - headers: `Authorization: Bearer <accessToken-teacher|assistant>`
  - response: `[EnrollmentResponse]`
- `POST /api/v1/courses/{courseId}/enrollments`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "userId": "...", "roleInCourse": "STUDENT" }`
  - response: `EnrollmentResponse`
- `POST /api/v1/courses/{courseId}/enrollments/by-email`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "emails": ["student@example.com"], "roleInCourse": "STUDENT" }`
  - response: `{ "addedOrUpdated": 1 }`
- `DELETE /api/v1/courses/{courseId}/enrollments/{userId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`

### Course Structure

- `GET /api/v1/courses/{courseId}/structure`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `CourseStructureResponse`
- `POST /api/v1/courses/{courseId}/blocks`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "title": "Week 1", "position": 10, "isVisible": true }`
  - response: `BlockResponse`
- `PUT /api/v1/courses/{courseId}/blocks/{blockId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "title": "Updated week", "position": 20, "isVisible": true }`
  - response: `BlockResponse`
- `DELETE /api/v1/courses/{courseId}/blocks/{blockId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
- `POST /api/v1/courses/{courseId}/items`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "blockId": "...", "itemType": "MATERIAL", "refId": "...", "position": 10, "isVisible": true }`
  - response: `ItemResponse`
- `PUT /api/v1/courses/{courseId}/items/{itemId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "blockId": null, "position": 20, "isVisible": false }`
  - response: `ItemResponse`
- `DELETE /api/v1/courses/{courseId}/items/{itemId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`

### Materials

- `POST /api/v1/courses/{courseId}/materials`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "title": "Lecture 1", "description": "intro", "body": "# Markdown", "blockId": "...", "position": 10, "isVisible": true, "attachmentIds": ["..."] }`
  - response: `MaterialResponse`
- `GET /api/v1/courses/{courseId}/materials`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `[MaterialResponse]`
- `GET /api/v1/materials/{materialId}`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `MaterialResponse`
- `PUT /api/v1/materials/{materialId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "title": "Updated", "clearBlock": false, "position": 20, "isVisible": false, "attachmentIds": [] }`
  - response: `MaterialResponse`
- `DELETE /api/v1/materials/{materialId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`

### Assignments

Поддерживаются типы:

- `TEXT`
- `FILE`
- `CODE`

- `POST /api/v1/courses/{courseId}/assignments`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `CreateAssignmentRequest`
  - response: `AssignmentResponse`
- `GET /api/v1/courses/{courseId}/assignments`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `[AssignmentResponse]`
- `GET /api/v1/assignments/{assignmentId}`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `AssignmentResponse`
- `PUT /api/v1/assignments/{assignmentId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `UpdateAssignmentRequest`
  - response: `AssignmentResponse`
- `DELETE /api/v1/assignments/{assignmentId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
- `POST /api/v1/assignments/{assignmentId}/publish`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - response: `AssignmentResponse`

Пример `CODE` assignment:

```json
{
  "title": "Implement stack",
  "assignmentType": "CODE",
  "workType": "INDIVIDUAL",
  "weight": 0.3,
  "position": 20,
  "isVisible": false,
  "code": {
    "repositoryName": "stack-assignment",
    "repositoryDescription": "Go assignment for students",
    "language": "GO",
    "maxAttempts": 3,
    "privateTestsAttachmentId": "00000000-0000-0000-0000-000000000000",
    "githubPat": "github_pat_optional_override"
  }
}
```

Для `CODE` assignment сервис создает приватный GitHub repository, кладет в него стартовый `.coderoom.yml` и сохраняет metadata репозитория и лимит попыток.

### Groups

- `GET /api/v1/courses/{courseId}/groups`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `[GroupResponse]`
- `POST /api/v1/courses/{courseId}/groups`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "name": "Team A" }`
  - response: `GroupResponse`
- `PUT /api/v1/courses/{courseId}/groups/{groupId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "name": "Updated team" }`
  - response: `GroupResponse`
- `DELETE /api/v1/courses/{courseId}/groups/{groupId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
- `POST /api/v1/courses/{courseId}/groups/{groupId}/members`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "userId": "..." }`
  - response: `GroupResponse`
- `DELETE /api/v1/courses/{courseId}/groups/{groupId}/members/{memberUserId}`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - response: `GroupResponse`

### Submissions

Для `TEXT|FILE` действует модель `single submission`.

- `POST /api/v1/assignments/{assignmentId}/submissions`
  - headers: `Authorization: Bearer <accessToken-student>`
  - body: `{ "textAnswer": "solution", "attachmentIds": ["..."] }`
  - response: `SubmissionResponse`
- `GET /api/v1/assignments/{assignmentId}/submissions`
  - headers: `Authorization: Bearer <accessToken-teacher|assistant>`
  - response: `[SubmissionResponse]`
- `GET /api/v1/assignments/{assignmentId}/submissions/me`
  - headers: `Authorization: Bearer <accessToken-student>`
  - response: `[SubmissionResponse]`
- `GET /api/v1/submissions/{submissionId}`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `SubmissionResponse`
- `PUT /api/v1/submissions/{submissionId}/grade`
  - headers: `Authorization: Bearer <accessToken-teacher|assistant>`
  - body: `{ "score": 95.00, "comment": "Well done" }`
  - response: `SubmissionResponse`

### Code Attempts

- `POST /api/v1/assignments/{assignmentId}/code-attempts`
  - headers: `Authorization: Bearer <accessToken-student>`
  - body: `{ "pullRequestUrl": "https://github.com/teacher/stack-assignment/pull/1" }`
  - response: `CodeAttemptResponse`
- `GET /api/v1/assignments/{assignmentId}/code-attempts`
  - headers: `Authorization: Bearer <accessToken-teacher|assistant>`
  - response: `[CodeAttemptResponse]`
- `GET /api/v1/assignments/{assignmentId}/code-attempts/me`
  - headers: `Authorization: Bearer <accessToken-student>`
  - response: `[CodeAttemptResponse]`
- `GET /api/v1/code-attempts/{attemptId}`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `CodeAttemptResponse`
- `GET /api/v1/code-attempts/{attemptId}/log`
  - headers: `Authorization: Bearer <accessToken>`
  - response: `CodeAttemptLogDownloadResponse`
- `POST /api/v1/code-attempts/{attemptId}/retry`
  - headers: `Authorization: Bearer <accessToken-teacher|assistant>`
  - response: `CodeAttemptResponse`

При создании попытки сервис валидирует `maxAttempts`, читает текущий `.coderoom.yml`, сохраняет config snapshot и публикует `AutogradeRequested` в Kafka.

### Gradebook

- `GET /api/v1/courses/{courseId}/gradebook`
  - headers: `Authorization: Bearer <accessToken-teacher|assistant>`
  - response: `CourseGradebookResponse`
- `GET /api/v1/courses/{courseId}/gradebook/me`
  - headers: `Authorization: Bearer <accessToken-student>`
  - response: `MyGradebookResponse`
- `GET /api/v1/courses/{courseId}/gradebook/csv`
  - headers: `Authorization: Bearer <accessToken-teacher|assistant>`
  - response: CSV file

Для `CODE` assignment в журнал попадает последняя попытка студента. Вклад задания считается как `score * assignment.weight`.

### Attachments

- `POST /api/v1/attachments/presign-upload`
  - headers: `Authorization: Bearer <accessToken>`
  - body: `{ "courseId": "...", "fileName": "lecture.pdf", "contentType": "application/pdf", "fileSize": 102400 }`
  - response: `PresignUploadAttachmentResponse`
- `POST /api/v1/attachments/presign-download`
  - headers: `Authorization: Bearer <accessToken>`
  - body: `{ "attachmentId": "..." }`
  - response: `PresignDownloadAttachmentResponse`

Private tests для code assignments тоже хранятся как course attachment, но не привязываются к видимому контенту.

### GitHub PAT

- `GET /api/v1/courses/{courseId}/github-pat`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - response: `GithubPatStatusResponse`
- `PUT /api/v1/courses/{courseId}/github-pat`
  - headers: `Authorization: Bearer <accessToken-teacher>`
  - body: `{ "token": "github_pat_..." }`
  - response: `GithubPatStatusResponse`
- `DELETE /api/v1/courses/{courseId}/github-pat`
  - headers: `Authorization: Bearer <accessToken-teacher>`

Токен хранится в Postgres в зашифрованном виде и никогда не возвращается через API.

## `.coderoom.yml`

MVP-конфиг, который кладется в template repository и снапшотится в code attempt:

```yaml
version: 1
language: go

check:
  type: unit_tests
  command: go test -json ./...
  workdir: .

attempts:
  max: 3

privateTests:
  enabled: true
  targetPath: .

scoring:
  strategy: pass_rate
  maxScore: 100.00
```

Дефолтный `privateTests.targetPath` зависит от языка:

- `GO` -> `.`
- `PYTHON` -> `tests`
- `JAVA` -> `src/test/java`

## Конфигурация

### DB

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### JWT

- `CODEROOM_JWT_SECRET`

### Kafka

- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `CODEROOM_KAFKA_TOPIC_AUTOGRADE_JOBS`
- `CODEROOM_KAFKA_TOPIC_AUTOGRADE_RESULTS`

### GitHub

- `CODEROOM_COURSE_GITHUB_PAT_KEY`
- `CODEROOM_GITHUB_API_BASE_URL`
- `CODEROOM_GITHUB_CLONE_BASE_URL`

### Межсервисные вызовы

- `CODEROOM_IDENTITY_BASE_URL`
- `CODEROOM_CONTENT_BASE_URL`
- `CODEROOM_CONTENT_BUCKET`
