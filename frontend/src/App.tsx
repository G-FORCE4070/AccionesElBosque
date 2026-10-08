import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import { LoginPage } from "./features/auth/pages/LoginPage";
import { MfaPage } from "./features/auth/pages/MfaPage";
import { RegisterPage } from "./features/register/pages/RegisterPage";
import { PaymentSuccessPage } from "./features/register/pages/PaymentSuccessPage";

import { AppShell } from "./components/layout/AppShell";
import { ProtectedRoute } from "./components/layout/ProtectedRoute";

import { DashboardPage } from "./features/dashboard/pages/DashboardPage";
import { PortfolioPage } from "./features/portfolio/pages/PortfolioPage";
import { OrdersPage } from "./features/orders/pages/OrdersPage";
import { TransactionsPage } from "./features/transactions/pages/TransactionsPage";
import { ReportsPage } from "./features/reports/pages/ReportsPage";
import { ProfilePage } from "./features/profile/pages/ProfilePage";

import { DevGallery } from "./routes/DevGallery";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Públicas */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/mfa" element={<MfaPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/payment/success" element={<PaymentSuccessPage />} />

        {/* Autenticadas */}
        <Route
          element={
            <ProtectedRoute>
              <AppShell />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/portfolio" element={<PortfolioPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>

        {/* Dev */}
        <Route path="/dev" element={<DevGallery />} />

        {/* Default */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
