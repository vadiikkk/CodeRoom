import type { PropsWithChildren } from 'react'
import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'

import { useAuthStore } from '@/features/auth/model/auth-store'
import { authApi } from '@/shared/api/auth'
import { hasStoredSession } from '@/shared/lib/storage'

export function SessionBootstrap({ children }: PropsWithChildren) {
  const { setAuthenticated, resetSession, setBootstrapping } = useAuthStore()
  const hasSession = hasStoredSession()

  const sessionQuery = useQuery({
    queryKey: ['session', 'me'],
    queryFn: authApi.me,
    enabled: hasSession,
    retry: false,
  })

  useEffect(() => {
    if (!hasSession) {
      resetSession()
      return
    }

    setBootstrapping()
  }, [hasSession, resetSession, setBootstrapping])

  useEffect(() => {
    if (sessionQuery.data) {
      setAuthenticated(sessionQuery.data)
    }
  }, [sessionQuery.data, setAuthenticated])

  useEffect(() => {
    if (sessionQuery.isError) {
      resetSession()
    }
  }, [sessionQuery.isError, resetSession])

  return children
}
