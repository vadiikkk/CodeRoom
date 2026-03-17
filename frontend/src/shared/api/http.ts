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
    const fallbackMessage =
      typeof data === 'string'
        ? data
        : `Request failed with status ${response.status}`

    throw new ApiError(fallbackMessage, response.status, data)
  }

  return data as T
}
