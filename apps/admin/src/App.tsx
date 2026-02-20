import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import ProtectedRoute from "./auth/ProtectedRoute";
import Layout from "./components/Layout";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import HymnListPage from "./pages/HymnListPage";
import HymnCreatePage from "./pages/HymnCreatePage";
import HymnEditPage from "./pages/HymnEditPage";
import AdminAssetUploadPage from "./pages/AdminAssetUploadPage";
import UserListPage from "./pages/UserListPage";
import InviteCodePage from "./pages/InviteCodePage";
import AdminEventPage from "./pages/AdminEventPage";
import ProfileChangeRequestPage from "./pages/ProfileChangeRequestPage";
import HelpPage from "./pages/HelpPage";
import AiRecommendationPage from "./pages/AiRecommendationPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/login/help" element={<HelpPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/hymns" element={<HymnListPage />} />
              <Route path="/hymns/new" element={<HymnCreatePage />} />
              <Route path="/hymns/:id/edit" element={<HymnEditPage />} />
              <Route path="/assets/upload" element={<AdminAssetUploadPage />} />
              <Route path="/users" element={<UserListPage />} />
              <Route
                path="/profile-change-requests"
                element={<ProfileChangeRequestPage />}
              />
              <Route path="/invite-codes" element={<InviteCodePage />} />
              <Route path="/events" element={<AdminEventPage />} />
              <Route path="/ai/recommendations" element={<AiRecommendationPage />} />
              <Route path="/help" element={<HelpPage />} />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
