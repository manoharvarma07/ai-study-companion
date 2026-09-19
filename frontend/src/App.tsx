import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { AdminRoute, ProtectedRoute, PublicOnlyRoute } from './auth/guards';
import { AppLayout } from './components/AppLayout';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { DashboardPage } from './pages/DashboardPage';
import { SpacesPage } from './pages/SpacesPage';
import { SpaceDetailPage } from './pages/SpaceDetailPage';
import { ProjectLayout, ProjectOverviewPage } from './pages/ProjectPages';
import { MaterialsPage } from './pages/MaterialsPage';
import { TutorPage } from './pages/TutorPage';
import { QuizListPage, QuizTakePage } from './pages/QuizPages';
import { MasteryPage } from './pages/MasteryPage';
import { GrowthPage } from './pages/GrowthPage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { AdminPage } from './pages/AdminPage';
import { NotFoundPage } from './pages/NotFoundPage';

function ProtectedShell() {
  return (
    <AppLayout>
      <Routes>
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="spaces" element={<SpacesPage />} />
        <Route path="spaces/:spaceId" element={<SpaceDetailPage />} />
        <Route path="projects/:projectId" element={<ProjectLayout />}>
          <Route index element={<ProjectOverviewPage />} />
          <Route path="materials" element={<MaterialsPage />} />
          <Route path="tutor" element={<TutorPage />} />
          <Route path="quiz" element={<QuizListPage />} />
          <Route path="mastery" element={<MasteryPage />} />
          <Route path="growth" element={<GrowthPage />} />
          <Route path="analytics" element={<AnalyticsPage />} />
        </Route>
        <Route path="quiz/:quizId" element={<QuizTakePage />} />
        <Route element={<AdminRoute />}>
          <Route path="admin" element={<AdminPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AppLayout>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
          </Route>
          <Route element={<ProtectedRoute />}>
            <Route path="/*" element={<ProtectedShell />} />
          </Route>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
