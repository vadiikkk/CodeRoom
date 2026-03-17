import type { PropsWithChildren } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

import { useAuthStore } from '@/features/auth/model/auth-store'
import { PageLoader } from '@/shared/ui/PageLoader'

export function ProtectedRoute({ children }: PropsWithChildren) {
  const location = useLocation()
  const { status, currentUser } = useAuthStore()

  if (status === 'loading') {
    return <PageLoader label="Проверяем доступ..." />
  }

  if (!currentUser) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return children
}
