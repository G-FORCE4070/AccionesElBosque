import type { ReactNode } from "react";
import { Check } from "lucide-react";
import { cn } from "../../../../lib/cn";

interface PlanCardProps {
  plan: "basic" | "premium";
  selected: boolean;
  onSelect: () => void;
}

function Feature({ children }: { children: ReactNode }) {
  return (
    <li className="flex items-center gap-2 text-sm text-text-secondary">
      <Check size={13} className="text-success shrink-0" strokeWidth={2.5} />
      {children}
    </li>
  );
}

export function PlanCard({ plan, selected, onSelect }: PlanCardProps) {
  const isBasic = plan === "basic";

  return (
    <button
      type="button"
      onClick={onSelect}
      className={cn(
        "w-full text-left p-5 rounded-2xl transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-gold/50",
        selected
          ? "border-2 border-gold bg-gold-subtle/20"
          : "border border-border-default bg-bg-tertiary/40 hover:border-gold/40"
      )}
    >
      {!isBasic && (
        <div className="flex justify-end mb-2">
          <span className="text-[10px] uppercase tracking-wide px-2 py-0.5 rounded-full bg-gold-subtle text-gold font-semibold">
            POPULAR
          </span>
        </div>
      )}

      <p className="text-xs text-text-muted mb-1">{isBasic ? "Básico" : "Premium"}</p>

      {isBasic ? (
        <p className="font-display text-3xl text-text-primary font-bold">Gratis</p>
      ) : (
        <div className="flex items-baseline gap-1">
          <span className="font-display text-3xl text-gold font-bold">$12</span>
          <span className="text-sm text-text-muted">/mes</span>
        </div>
      )}

      <ul className="mt-4 space-y-2">
        {isBasic ? (
          <>
            <Feature>Acceso mercados globales</Feature>
            <Feature>Comisionista (opcional)</Feature>
            <Feature>Datos en tiempo real</Feature>
          </>
        ) : (
          <>
            <Feature>Alertas de precios</Feature>
            <Feature>Watchlist personalizada</Feature>
            <Feature>Todo lo del plan Básico</Feature>
          </>
        )}
      </ul>

      {!isBasic && (
        <p className="mt-3 text-xs text-gold-light">$120/año (2 meses gratis)</p>
      )}
    </button>
  );
}
