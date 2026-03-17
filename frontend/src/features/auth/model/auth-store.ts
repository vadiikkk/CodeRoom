import { create } from 'zustand'

import type { MeResponse } from '@/entities/auth/types'
import { clearStoredTokens } from '@/shared/lib/storage'

type SessionStatus = 'loading' | 'authenticated' | 'anonymous'

interface AuthState {
  status: SessionStatus
  currentUser: MeResponse | null
  setBootstrapping: () => void
  setAuthenticated: (user: MeResponse) => void
  resetSession: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  status: 'loading',
  currentUser: null,
  setBootstrapping: () => {
    set({ status: 'loading' })
  },
  setAuthenticated: (user) => {
    set({
      status: 'authenticated',
      currentUser: user,
    })
  },
  resetSession: () => {
    clearStoredTokens()
    set({
      status: 'anonymous',
      currentUser: null,
    })
  },
}))
