import { useEffect } from "react";
import { Outlet, useLocation } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { useSessionStore } from "../../store/sessionStore";
import { useProfileStore } from "../../features/profile/store/profileStore";
import { useTradingStore } from "../../features/dashboard/store/tradingStore";

const ROUTE_LABELS: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/portfolio": "Portafolio",
  "/orders": "Órdenes",
  "/transactions": "Transacciones",
  "/reports": "Reportes",
  "/profile": "Gestión de Perfil",
};

export function AppShell() {
  const { pathname } = useLocation();
  const breadcrumb = ROUTE_LABELS[pathname] ?? "";
  const token = useSessionStore((s) => s.token ?? "");
  const loaded = useProfileStore((s) => s.loaded);
  const loadProfile = useProfileStore((s) => s.loadProfile);
  const loadPortfolio = useTradingStore((s) => s.loadPortfolio);

  useEffect(() => {
    if (token && !loaded) {
      loadProfile(token);
    }
  }, [token, loaded, loadProfile]);

  useEffect(() => {
    if (token) {
      loadPortfolio(token);
    }
  }, [token, loadPortfolio]);

  return (
    <div className="min-h-screen bg-bg-primary bg-grid-subtle bg-grid">
      <Sidebar />
      <main className="ml-[240px] min-h-screen flex flex-col">
        {breadcrumb && (
          <div className="px-8 pt-5 text-xs text-text-subtle">{breadcrumb}</div>
        )}
        <div className="px-8 py-6 flex-1">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
