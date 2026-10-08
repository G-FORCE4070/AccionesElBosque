import {
  LayoutDashboard,
  Wallet,
  FileText,
  BarChart3,
  FileBarChart,
  UserCog,
} from "lucide-react";
import { Logo } from "../ui/Logo";
import { SidebarItem } from "./SidebarItem";
import { SidebarUserBlock } from "./SidebarUserBlock";

const NAV_ITEMS = [
  { icon: <LayoutDashboard size={18} />, label: "Dashboard", to: "/dashboard" },
  { icon: <Wallet size={18} />, label: "Portafolio", to: "/portfolio" },
  { icon: <FileText size={18} />, label: "Órdenes", to: "/orders" },
  { icon: <BarChart3 size={18} />, label: "Transacciones", to: "/transactions" },
  { icon: <FileBarChart size={18} />, label: "Reportes", to: "/reports", disabled: true },
  { icon: <UserCog size={18} />, label: "Gestión de Perfil", to: "/profile" },
];

export function Sidebar() {
  return (
    <aside className="fixed left-0 top-0 h-screen w-[240px] bg-bg-secondary border-r border-border-subtle flex flex-col p-5 z-40">
      {/* Logo */}
      <div className="pb-8">
        <Logo iconSize={22} />
      </div>

      {/* Navigation */}
      <nav className="flex-1 flex flex-col gap-1 overflow-y-auto">
        {NAV_ITEMS.map((item) => (
          <SidebarItem key={item.to} icon={item.icon} label={item.label} to={item.to} disabled={item.disabled} />
        ))}
      </nav>

      {/* User block */}
      <SidebarUserBlock />
    </aside>
  );
}
