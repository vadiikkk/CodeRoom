export type GlobalRole = 'STUDENT' | 'ASSISTANT' | 'TEACHER'

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
}

export interface RefreshRequest {
  refreshToken: string
}

export interface MeResponse {
  userId: string
  email: string
  role: GlobalRole
}

export interface AdminUserResponse {
  userId: string
  email: string
  role: GlobalRole
  isRoot: boolean
  isActive: boolean
  createdAt: string
}

export interface SetUserRoleRequest {
  email: string
  role: GlobalRole
}

export interface SetUserActiveRequest {
  email: string
  isActive: boolean
}

export interface LookupUsersByEmailRequest {
  emails: string[]
}

export interface LookupUsersByIdRequest {
  userIds: string[]
}

export interface LookupUsersResponse {
  users: Array<{ userId: string; email: string }>
}
