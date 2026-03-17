# CodeRoom Frontend

SPA-клиент для LMS-платформы CodeRoom.

## Стек

- `React 18`
- `TypeScript`
- `Vite`
- `React Router`
- `TanStack Query`
- `Zustand`
- `react-hook-form + zod`
- `Tailwind CSS v4`

## Запуск

```bash
npm install
npm run dev
```

По умолчанию frontend ходит в `http://localhost:8080`.

## Настройка окружения

Поддерживается переменная:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

## Структура

- `src/app` — bootstrap, providers, router, shell
- `src/shared` — api client, config, ui, utils
- `src/entities` — типы домена
- `src/features` — auth store и прикладные сценарии
- `src/processes` — route guards и bootstrap сессии
- `src/pages` — route-level экраны
