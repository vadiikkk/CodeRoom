# CodeRoom Frontend

SPA для LMS CodeRoom: курсы, материалы, задания (текст, файлы, код), проверка, ведомости, профиль. Запросы идут через **API Gateway** (JWT, refresh при 401, presigned URL для файлов в S3/MinIO).

Точные версии зависимостей - в `package.json`.

## Стек

- **React 18**, **TypeScript**, **Vite 5**
- **React Router 7**
- **TanStack Query** - данные с сервера, кэш, поллинг статуса code-заданий
- **Zustand** - сессия (минимальное клиентское состояние)
- **react-hook-form** + **Zod** - формы и валидация
- **Tailwind CSS v4** (`@tailwindcss/vite`)

## Требования

- Node.js 20+ (рекомендуется LTS)
- npm (или совместимый менеджер пакетов)

## Установка и скрипты

```bash
npm install
npm run dev      # dev-сервер Vite (порт см. в консоли, обычно 5173)
npm run build    # tsc -b && vite build → dist/
npm run preview  # локальный просмотр production-сборки
npm run lint     # ESLint
```

## API и окружение

По умолчанию клиент обращается к **`http://localhost:8080`** (совпадает с типичным портом gateway в репозитории).

Переопределение:

```bash
# скопируйте пример и при необходимости измените URL
cp .env.example .env
```

```bash
VITE_API_BASE_URL=http://localhost:8080
```

Бэкенд и gateway должны быть запущены отдельно (см. `backend/README.md`).

## Структура `src/`

| Путь | Назначение |
|------|------------|
| `app/` | `App`, провайдеры (`QueryClient`), роутер, `AppShell` (навигация) |
| `pages/` | Экраны: логин, регистрация, курсы, управление курсом, материалы, задания, проверка, ведомости, профиль, админка |
| `entities/` | Типы домена (auth, course, submission и т.д.) |
| `features/` | Прикладная логика (например, store авторизации) |
| `processes/` | Защищённые маршруты, восстановление сессии по токенам (`SessionBootstrap`) |
| `shared/` | HTTP-клиент (`api/*`), `config/env`, UI-компоненты, утилиты (`format`, `errors`, загрузка вложений) |

Маршруты приложения задаются в `src/app/router.tsx`.
