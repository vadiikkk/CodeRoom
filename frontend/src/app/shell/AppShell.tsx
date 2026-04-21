import { useMutation } from '@tanstack/react-query'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'

import { authApi } from '@/shared/api/auth'
import { Button } from '@/shared/ui/Button'
import { PageLoader } from '@/shared/ui/PageLoader'
import { useAuthStore } from '@/features/auth/model/auth-store'

export function AppShell() {
  const navigate = useNavigate()
  const { currentUser, status, resetSession } = useAuthStore()

  const logoutMutation = useMutation({
    mutationFn: async () => {
      await authApi.logout()
    },
    onSettled: () => {
      resetSession()
      navigate('/login', { replace: true })
    },
  })

  if (status === 'loading' || !currentUser) {
    return <PageLoader label="Загружаем сессию..." />
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-800/80 bg-slate-950/70 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-6 px-6 py-4">
          <div className="flex items-center gap-6">
            <Link to="/app/courses" className="text-xl font-semibold text-white">
              CodeRoom
            </Link>
            <nav className="flex items-center gap-2 text-sm text-slate-300">
              <NavLink
                to="/app/courses"
                className={({ isActive }) =>
                  isActive
                    ? 'rounded-full bg-blue-500/15 px-3 py-2 text-blue-200'
                    : 'rounded-full px-3 py-2 text-slate-300 transition hover:bg-slate-800/80 hover:text-white'
                }
              >
                Курсы
              </NavLink>
              {currentUser.role === 'TEACHER' ? (
                <NavLink
                  to="/app/admin"
                  className={({ isActive }) =>
                    isActive
                      ? 'rounded-full bg-blue-500/15 px-3 py-2 text-blue-200'
                      : 'rounded-full px-3 py-2 text-slate-300 transition hover:bg-slate-800/80 hover:text-white'
                  }
                >
                  Администрирование
                </NavLink>
              ) : null}
            </nav>
          </div>

          <div className="flex items-center gap-4">
            <Link
              to="/app/profile"
              className="hidden text-right transition hover:opacity-80 sm:block"
            >
              <div className="text-sm font-medium text-white">{currentUser.email}</div>
              <div className="text-xs uppercase tracking-wide text-slate-400">
                {currentUser.role}
              </div>
            </Link>
            <Button
              variant="secondary"
              onClick={() => logoutMutation.mutate()}
              isLoading={logoutMutation.isPending}
            >
              Выйти
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto flex max-w-7xl flex-col px-6 py-8">
        <Outlet />
      </main>
    </div>
  )
}
