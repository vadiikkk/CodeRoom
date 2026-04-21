import type { AuthTokens } from '@/entities/auth/types'
import { env } from '@/shared/config/env'
import {
  clearStoredTokens,
  getStoredTokens,
  setStoredTokens,
} from '@/shared/lib/storage'

interface RequestOptions extends RequestInit {
  auth?: boolean
  retryOnUnauthorized?: boolean
}

export class ApiError extends Error {
  status: number
  data: unknown

  constructor(message: string, status: number, data: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

let refreshPromise: Promise<AuthTokens | null> | null = null

function buildUrl(path: string) {
  return `${env.apiBaseUrl}${path}`
}

async function parseResponse(response: Response) {
  const text = await response.text()

  if (!text) {
    return null
  }

  try {
    return JSON.parse(text) as unknown
  } catch {
    return text
  }
}

async function refreshTokens() {
  const storedTokens = getStoredTokens()

  if (!storedTokens?.refreshToken) {
    clearStoredTokens()
    return null
  }

  if (!refreshPromise) {
    refreshPromise = fetch(buildUrl('/api/v1/auth/refresh'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        refreshToken: storedTokens.refreshToken,
      }),
    })
      .then(async (response) => {
        const data = (await parseResponse(response)) as AuthTokens | null

        if (!response.ok || !data) {
          clearStoredTokens()
          return null
        }

        setStoredTokens(data)
        return data
      })
      .catch(() => {
        clearStoredTokens()
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const {
    auth = true,
    retryOnUnauthorized = true,
    headers,
    body,
    ...init
  } = options

  const storedTokens = getStoredTokens()
  const requestHeaders = new Headers(headers)

  if (body && !(body instanceof FormData) && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json')
  }

  if (auth && storedTokens?.accessToken) {
    requestHeaders.set(
      'Authorization',
      `${storedTokens.tokenType} ${storedTokens.accessToken}`,
    )
  }

  const response = await fetch(buildUrl(path), {
    ...init,
    headers: requestHeaders,
    body,
  })

  if (response.status === 401 && auth && retryOnUnauthorized) {
    const refreshedTokens = await refreshTokens()

    if (refreshedTokens?.accessToken) {
      return apiRequest<T>(path, {
        ...options,
        retryOnUnauthorized: false,
      })
    }

    clearStoredTokens()
  }

  const data = await parseResponse(response)

  if (!response.ok) {
    let message: string | undefined

    if (typeof data === 'object' && data !== null && 'message' in data) {
      const msg = (data as { message?: unknown }).message
      if (typeof msg === 'string' && msg.trim()) message = msg
    }

    if (!message && typeof data === 'string' && data.trim()) {
      message = data
    }

    if (!message) {
      message = httpStatusMessage(response.status)
    }

    throw new ApiError(message, response.status, data)
  }

  return data as T
}

function httpStatusMessage(status: number): string {
  switch (status) {
    case 400:
      return 'Некорректный запрос'
    case 401:
      return 'Неверный логин или пароль'
    case 403:
      return 'Доступ запрещён'
    case 404:
      return 'Ресурс не найден'
    case 409:
      return 'Конфликт данных'
    case 422:
      return 'Ошибка валидации'
    case 429:
      return 'Слишком много запросов, попробуйте позже'
    case 500:
      return 'Внутренняя ошибка сервера'
    case 502:
      return 'Сервер временно недоступен'
    case 503:
      return 'Сервис временно недоступен'
    default:
      return `Ошибка сервера (${status})`
  }
}
