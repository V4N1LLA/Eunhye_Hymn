import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import ProtectedRoute from "./auth/ProtectedRoute";
import Layout from "./components/Layout";
import LoginPage from "./pages/LoginPage";
import HymnListPage from "./pages/HymnListPage";
import HymnCreatePage from "./pages/HymnCreatePage";
import HymnEditPage from "./pages/HymnEditPage";
import AdminAssetUploadPage from "./pages/AdminAssetUploadPage";
import UserListPage from "./pages/UserListPage";
import InviteCodePage from "./pages/InviteCodePage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/hymns" element={<HymnListPage />} />
              <Route path="/hymns/new" element={<HymnCreatePage />} />
              <Route path="/hymns/:id/edit" element={<HymnEditPage />} />
              <Route path="/assets/upload" element={<AdminAssetUploadPage />} />
              <Route path="/users" element={<UserListPage />} />
              <Route path="/invite-codes" element={<InviteCodePage />} />
              <Route path="/" element={<Navigate to="/hymns" replace />} />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
