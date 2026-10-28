import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getUser, isLoggedIn } from "../auth";

// Usage: <Route element={<ProtectedRoute />}>            -> any logged-in user
//        <Route element={<ProtectedRoute roles={[...]} />}> -> only those roles
function ProtectedRoute({ roles }) {
  const location = useLocation();

  if (!isLoggedIn()) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (roles && !roles.includes(getUser()?.role)) {
    return <Navigate to="/home" replace />;
  }

  return <Outlet />;
}

export default ProtectedRoute;