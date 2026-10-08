import type { ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { cn } from "../../lib/cn";

interface SidebarItemProps {
  readonly icon: ReactNode;
  readonly label: string;
  readonly to: string;
  readonly disabled?: boolean;
}

export function SidebarItem({ icon, label, to, disabled }: SidebarItemProps) {
  if (disabled) {
    return (
      <div
        className="flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-text-muted/40 cursor-not-allowed select-none"
        title="Próximamente"
      >
        <span className="shrink-0">{icon}</span>
        {label}
      </div>
    );
  }

  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          "flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-gold/40",
          isActive
            ? "bg-gold-subtle border border-gold/30 text-gold"
            : "text-text-muted hover:bg-bg-tertiary/60 hover:text-text-secondary"
        )
      }
    >
      <span className="shrink-0">{icon}</span>
      {label}
    </NavLink>
  );
}
