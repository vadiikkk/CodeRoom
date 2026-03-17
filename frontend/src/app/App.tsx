import { BrowserRouter } from 'react-router-dom'

import { AppProviders } from '@/app/providers/AppProviders'
import { AppRoutes } from '@/app/router'
import { SessionBootstrap } from '@/processes/session/SessionBootstrap'

export function App() {
  return (
    <AppProviders>
      <BrowserRouter>
        <SessionBootstrap>
          <AppRoutes />
        </SessionBootstrap>
      </BrowserRouter>
    </AppProviders>
  )
}
