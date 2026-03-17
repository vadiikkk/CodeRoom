const ACCESS_TOKEN_KEY = 'coderoom.accessToken'
const REFRESH_TOKEN_KEY = 'coderoom.refreshToken'
const TOKEN_TYPE_KEY = 'coderoom.tokenType'

export interface StoredAuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
}

export function getStoredTokens(): StoredAuthTokens | null {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY)
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY)
  const tokenType = localStorage.getItem(TOKEN_TYPE_KEY)

  if (!accessToken || !refreshToken || !tokenType) {
    return null
  }

  return {
    accessToken,
    refreshToken,
    tokenType,
  }
}

export function hasStoredSession() {
  return Boolean(getStoredTokens()?.refreshToken)
}

export function setStoredTokens(tokens: StoredAuthTokens) {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
  localStorage.setItem(TOKEN_TYPE_KEY, tokens.tokenType)
}

export function clearStoredTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(TOKEN_TYPE_KEY)
}
