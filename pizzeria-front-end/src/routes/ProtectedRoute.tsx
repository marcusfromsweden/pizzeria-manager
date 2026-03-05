import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { usePizzeriaCode } from '../hooks/usePizzeriaCode';

export const ProtectedRoute = () => {
  const { isAuthenticated, isLoading } = useAuth();
  const pizzeriaCode = usePizzeriaCode();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-600 border-t-transparent" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <Navigate
        to={`/${pizzeriaCode}/login`}
        state={{ from: location }}
        replace
      />
    );
  }

  return <Outlet />;
};

export default ProtectedRoute;
