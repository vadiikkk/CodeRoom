import { Navigate, Route, Routes } from 'react-router-dom'

import { AppShell } from '@/app/shell/AppShell'
import { AdminPage } from '@/pages/app/AdminPage'
import { AssignmentDetailsPage } from '@/pages/app/AssignmentDetailsPage'
import { CourseDetailsPage } from '@/pages/app/CourseDetailsPage'
import { CourseManagePage } from '@/pages/app/CourseManagePage'
import { CoursesPage } from '@/pages/app/CoursesPage'
import { GradingPage } from '@/pages/app/GradingPage'
import { MaterialDetailsPage } from '@/pages/app/MaterialDetailsPage'
import { ProfilePage } from '@/pages/app/ProfilePage'
import { StaffGradebookPage } from '@/pages/app/StaffGradebookPage'
import { StudentGradebookPage } from '@/pages/app/StudentGradebookPage'
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
        <Route path="courses/:courseId/gradebook" element={<StaffGradebookPage />} />
        <Route path="courses/:courseId/grades" element={<StudentGradebookPage />} />
        <Route
          path="courses/:courseId/assignments/:assignmentId/grading"
          element={<GradingPage />}
        />
        <Route
          path="courses/:courseId/materials/:materialId"
          element={<MaterialDetailsPage />}
        />
        <Route
          path="courses/:courseId/assignments/:assignmentId"
          element={<AssignmentDetailsPage />}
        />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="admin" element={<AdminPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/app/courses" replace />} />
    </Routes>
  )
}
