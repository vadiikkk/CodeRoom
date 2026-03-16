# content-service

Тонкий internal-сервис для работы с MinIO/S3: presigned upload/download, удаление объектов и прямой byte-transfer для backend-воркеров.

## Важные правила

- Сервис не проксируется через `api-gateway` и не предназначен для фронта.
- Бизнес-права доступа не проверяет: RBAC остается в `course-service`.
- Разделяет internal и public endpoint MinIO/S3, чтобы presigned URL корректно работали и локально, и в Docker, и в проде.
- Direct transfer ручки нужны для backend-сервисов внутри Docker-сети, когда public presigned URL с `localhost` использовать нельзя.

## API

Все ручки internal-only:

- headers: обычно без `Authorization`, вызов идет из backend-сервисов внутри сети

### Objects

- `POST /internal/api/v1/objects/presign-upload`
  - headers: internal service call
  - body: `{ "objectKey": "courses/<courseId>/attachments/<attachmentId>/lecture.pdf", "contentType": "application/pdf" }`
  - response: `{ "url": "http://localhost:9000/...signature...", "method": "PUT" }`
- `POST /internal/api/v1/objects/presign-download`
  - headers: internal service call
  - body: `{ "objectKey": "courses/<courseId>/attachments/<attachmentId>/lecture.pdf", "fileName": "lecture.pdf" }`
  - response: `{ "url": "http://localhost:9000/...signature...", "method": "GET" }`
- `DELETE /internal/api/v1/objects`
  - headers: internal service call
  - body: `{ "objectKey": "courses/<courseId>/attachments/<attachmentId>/lecture.pdf" }`

### Internal direct transfer

- `POST /internal/api/v1/objects/download`
  - headers: internal service call
  - body: `{ "objectKey": "..." }`
  - response: raw bytes
- `POST /internal/api/v1/objects/upload?objectKey=...&contentType=text/plain`
  - headers: internal service call
  - body: raw bytes

## Конфигурация

- `CODEROOM_STORAGE_INTERNAL_ENDPOINT` - внутренний URL MinIO/S3, например `http://minio:9000`
- `CODEROOM_STORAGE_PUBLIC_ENDPOINT` - внешний URL MinIO/S3 для presigned URL, например `http://localhost:9000`
- `CODEROOM_STORAGE_ACCESS_KEY`
- `CODEROOM_STORAGE_SECRET_KEY`
- `CODEROOM_STORAGE_BUCKET`
- `CODEROOM_STORAGE_REGION`
- `CODEROOM_STORAGE_UPLOAD_EXPIRY_MINUTES`
- `CODEROOM_STORAGE_DOWNLOAD_EXPIRY_MINUTES`
