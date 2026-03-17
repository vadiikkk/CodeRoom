import type {
  AuthTokens,
  LoginRequest,
  MeResponse,
  RegisterRequest,
} from '@/entities/auth/types'
import { apiRequest } from '@/shared/api/http'
import { getStoredTokens } from '@/shared/lib/storage'

export const authApi = {
  login(payload: LoginRequest) {
    return apiRequest<AuthTokens>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
      auth: false,
      retryOnUnauthorized: false,
    })
  },

  register(payload: RegisterRequest) {
    return apiRequest<AuthTokens>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
      auth: false,
      retryOnUnauthorized: false,
    })
  },

  me() {
    return apiRequest<MeResponse>('/api/v1/me')
  },

  async logout() {
    const storedTokens = getStoredTokens()

    if (!storedTokens?.refreshToken) {
      return
    }

    await apiRequest<void>('/api/v1/auth/logout', {
      method: 'POST',
      body: JSON.stringify({
        refreshToken: storedTokens.refreshToken,
      }),
      auth: false,
      retryOnUnauthorized: false,
    })
  },
}
