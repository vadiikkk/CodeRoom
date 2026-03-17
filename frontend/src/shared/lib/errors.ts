import { ApiError } from '@/shared/api/http'

export function getErrorMessage(error: unknown, fallback = 'Что-то пошло не так') {
  if (error instanceof ApiError) {
    if (typeof error.data === 'object' && error.data !== null && 'message' in error.data) {
      const message = error.data.message

      if (typeof message === 'string' && message.trim()) {
        return message
      }
    }

    if (error.message.trim()) {
      return error.message
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return fallback
}
