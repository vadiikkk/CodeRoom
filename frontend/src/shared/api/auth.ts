import type {
  AdminUserResponse,
  AuthTokens,
  LoginRequest,
  LookupUsersByEmailRequest,
  LookupUsersByIdRequest,
  LookupUsersResponse,
  MeResponse,
  RegisterRequest,
  SetUserActiveRequest,
  SetUserRoleRequest,
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

  adminListUsers(query = '', page = 0, size = 50) {
    const params = new URLSearchParams({ q: query, page: String(page), size: String(size) })
    return apiRequest<AdminUserResponse[]>(`/api/v1/admin/users?${params}`)
  },

  adminSetRole(payload: SetUserRoleRequest) {
    return apiRequest<void>('/api/v1/admin/users/role', {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  adminSetActive(payload: SetUserActiveRequest) {
    return apiRequest<void>('/api/v1/admin/users/active', {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  lookupByEmails(payload: LookupUsersByEmailRequest) {
    return apiRequest<LookupUsersResponse>('/api/v1/users/lookup', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  lookupByIds(payload: LookupUsersByIdRequest) {
    return apiRequest<LookupUsersResponse>('/api/v1/users/lookup-by-ids', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  changePassword(oldPassword: string, newPassword: string) {
    return apiRequest<void>('/api/v1/me/password', {
      method: 'POST',
      body: JSON.stringify({ oldPassword, newPassword }),
    })
  },

  async logoutAll() {
    const storedTokens = getStoredTokens()
    if (!storedTokens?.refreshToken) return

    await apiRequest<void>('/api/v1/auth/logout-all', {
      method: 'POST',
      body: JSON.stringify({ refreshToken: storedTokens.refreshToken }),
      auth: false,
      retryOnUnauthorized: false,
    })
  },
}
