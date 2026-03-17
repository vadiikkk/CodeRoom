import { Navigate, Route, Routes } from 'react-router-dom'

import { AppShell } from '@/app/shell/AppShell'
import { AssignmentDetailsPage } from '@/pages/app/AssignmentDetailsPage'
import { CourseDetailsPage } from '@/pages/app/CourseDetailsPage'
import { CourseManagePage } from '@/pages/app/CourseManagePage'
import { CoursesPage } from '@/pages/app/CoursesPage'
import { MaterialDetailsPage } from '@/pages/app/MaterialDetailsPage'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { ProtectedRoute } from '@/processes/routing/ProtectedRoute'
import { PublicOnlyRoute } from '@/processes/routing/PublicOnlyRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicOnlyRoute>
            <LoginPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path="/register"
        element={
          <PublicOnlyRoute>
            <RegisterPage />
          </PublicOnlyRoute>
        }
      />

      <Route
        path="/app"
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/app/courses" replace />} />
        <Route path="courses" element={<CoursesPage />} />
        <Route path="courses/:courseId" element={<CourseDetailsPage />} />
        <Route path="courses/:courseId/manage" element={<CourseManagePage />} />
        <Route
          path="courses/:courseId/materials/:materialId"
          element={<MaterialDetailsPage />}
        />
        <Route
          path="courses/:courseId/assignments/:assignmentId"
          element={<AssignmentDetailsPage />}
        />
      </Route>

      <Route path="*" element={<Navigate to="/app/courses" replace />} />
    </Routes>
  )
}
