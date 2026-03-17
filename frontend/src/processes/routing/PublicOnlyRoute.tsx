import type { PropsWithChildren } from 'react'
import { Navigate } from 'react-router-dom'

import { useAuthStore } from '@/features/auth/model/auth-store'
import { PageLoader } from '@/shared/ui/PageLoader'

export function PublicOnlyRoute({ children }: PropsWithChildren) {
  const { status, currentUser } = useAuthStore()

  if (status === 'loading') {
    return <PageLoader label="Загружаем..." />
  }

  if (currentUser) {
    return <Navigate to="/app/courses" replace />
  }

  return children
}
