import { cn } from "../../../lib/cn";
import type { Holding } from "../data/mockHoldings";

const fmt = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

interface PortfolioDetailViewProps {
  holdings: Holding[];
}

export function PortfolioDetailView({ holdings }: PortfolioDetailViewProps) {
  return (
    <div className="grid grid-cols-2 gap-4">
      {holdings.map((h) => {
        const isPos = h.gainDollars == null || h.gainDollars >= 0;
        const gpColor = h.gainDollars == null ? "text-text-muted" : isPos ? "text-success" : "text-danger";
        const gpBg    = h.gainDollars == null
          ? "bg-bg-elevated border-border-subtle"
          : isPos ? "bg-success/10 border-success/20" : "bg-danger/10 border-danger/20";
        const barPct  = h.gainPct != null ? Math.min(100, Math.abs(h.gainPct) * 4) : 0;

        return (
          <div
            key={h.symbol}
            className="bg-bg-tertiary rounded-2xl p-5 border border-border-subtle flex flex-col gap-4"
          >
            {/* Header */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-gold-subtle border border-gold/20 flex items-center justify-center text-sm font-bold text-gold">
                  {h.symbol.slice(0, 2)}
                </div>
                <div>
                  <p className="text-sm font-bold text-text-primary">{h.symbol}</p>
                  <p className="text-xs text-text-muted">{h.quantity} acciones</p>
                </div>
              </div>
              <span className={cn("text-xs font-semibold px-2.5 py-1 rounded-full border", gpBg, gpColor)}>
                {h.gainPct != null
                  ? `${h.gainPct >= 0 ? "+" : ""}${h.gainPct.toFixed(2)}%`
                  : "—"}
              </span>
            </div>

            {/* Prices */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <p className="text-[10px] uppercase tracking-wide text-text-muted mb-1">Precio actual</p>
                <p className="text-base font-bold text-text-primary tabular-nums">
                  {h.currentPrice != null ? `$${fmt.format(h.currentPrice)}` : "—"}
                </p>
              </div>
              <div>
                <p className="text-[10px] uppercase tracking-wide text-text-muted mb-1">Valor total</p>
                <p className="text-base font-bold text-text-primary tabular-nums">
                  {h.totalValue != null ? `$${fmt.format(h.totalValue)}` : "—"}
                </p>
              </div>
            </div>

            {/* G/P bar */}
            <div>
              <div className="flex justify-between text-xs mb-1.5">
                <span className="text-text-muted">G/P</span>
                <span className={cn("font-semibold tabular-nums", gpColor)}>
                  {h.gainDollars != null
                    ? `${h.gainDollars >= 0 ? "+" : ""}$${fmt.format(Math.abs(h.gainDollars))}`
                    : "—"}
                </span>
              </div>
              <div className="h-1.5 rounded-full bg-bg-elevated overflow-hidden">
                <div
                  className={cn(
                    "h-full rounded-full transition-all duration-500",
                    h.gainDollars == null ? "bg-text-muted/30" : isPos ? "bg-success" : "bg-danger"
                  )}
                  style={{ width: `${barPct}%` }}
                />
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
