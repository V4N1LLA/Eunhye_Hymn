import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";
import NotAuthorizedPage from "../pages/NotAuthorizedPage";

export default function ProtectedRoute() {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (user?.role !== "ADMIN") {
    return <NotAuthorizedPage />;
  }

  return <Outlet />;
}
