import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-cream dark:bg-bark">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-stone-300 border-t-sage-600 dark:border-stone-700 dark:border-t-sage-400" />
      </div>
    );
  }
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return null;
  if (isAuthenticated) return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}

export function AdminRoute() {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-cream dark:bg-bark">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-stone-300 border-t-forest-600 dark:border-stone-700 dark:border-t-forest-400" />
      </div>
    );
  }
  if (user?.role !== 'ADMIN') return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}
