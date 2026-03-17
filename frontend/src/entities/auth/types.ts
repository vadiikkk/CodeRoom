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
