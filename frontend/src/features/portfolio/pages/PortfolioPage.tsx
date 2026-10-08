import { useEffect, useMemo, useState } from "react";
import { PortfolioToolbar, type SortKey, type ViewMode } from "../components/PortfolioToolbar";
import { PortfolioTable } from "../components/PortfolioTable";
import { PortfolioBarChart } from "../components/PortfolioBarChart";
import { PortfolioDetailView } from "../components/PortfolioDetailView";
import { useSessionStore } from "../../../store/sessionStore";
import { useProfileStore } from "../../profile/store/profileStore";
import { getPortfolio, type HoldingItem } from "../../dashboard/services/tradingService";

const fmt = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function buildComparator(sortBy: SortKey): (a: HoldingItem, b: HoldingItem) => number {
  switch (sortBy) {
    case "symbol":   return (a, b) => a.symbol.localeCompare(b.symbol);
    case "quantity": return (a, b) => b.quantity - a.quantity;
    case "gainDollars": return (a, b) => (b.gainDollars ?? -Infinity) - (a.gainDollars ?? -Infinity);
    case "gainPct":  return (a, b) => (b.gainPct ?? -Infinity) - (a.gainPct ?? -Infinity);
    default:         return (a, b) => (b.totalValue ?? -Infinity) - (a.totalValue ?? -Infinity);
  }
}

export function PortfolioPage() {
  const token = useSessionStore((s) => s.token);
  const portfolioPrefs = useProfileStore((s) => s.portfolio);

  const [sortBy, setSortBy] = useState<SortKey>(
    (portfolioPrefs.defaultOrder as SortKey) || "totalValue"
  );
  const [view, setView] = useState<ViewMode>(
    (portfolioPrefs.viewType as ViewMode) || "list"
  );

  const [holdings, setHoldings] = useState<HoldingItem[]>([]);
  const [balance, setBalance] = useState<number | null>(null);
  const [pricesAvailable, setPricesAvailable] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    setLoading(true);
    setError(null);
    getPortfolio(token)
      .then((data) => {
        setBalance(data.balance);
        setPricesAvailable(data.pricesAvailable);
        setHoldings(data.holdings);
      })
      .catch(() => setError("No se pudo cargar el portafolio. Intenta de nuevo."))
      .finally(() => setLoading(false));
  }, [token]);

  const filtered = useMemo(() => {
    if (!portfolioPrefs.showLowWeight) return holdings;
    return holdings.filter((h) => h.totalValue == null || h.totalValue >= 10);
  }, [holdings, portfolioPrefs.showLowWeight]);

  const sorted = useMemo(
    () => [...filtered].sort(buildComparator(sortBy)),
    [filtered, sortBy]
  );

  return (
    <div>
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="font-display text-4xl font-bold text-text-primary">Portafolio</h1>
          <p className="text-sm text-text-muted mt-1">Vista general de tus inversiones</p>
        </div>
        <div className="flex items-center gap-2 bg-bg-secondary border border-border-subtle rounded-full px-6 py-3">
          <span className="text-xs text-text-muted">Saldo disponible</span>
          <span className="text-2xl font-bold text-gold tabular-nums">
            {balance != null ? `$${fmt.format(balance)}` : "—"}
          </span>
          <span className="text-xs text-text-muted">USD</span>
        </div>
      </div>

      {!pricesAvailable && (
        <div className="mb-4 flex items-center gap-2 bg-yellow-500/10 border border-yellow-500/30 rounded-xl px-4 py-3">
          <span className="text-yellow-400 text-sm font-medium">
            ⚠ Precios no disponibles en este momento. Los valores de mercado se muestran como —.
          </span>
        </div>
      )}

      {error && (
        <div className="mb-4 flex items-center gap-2 bg-danger/10 border border-danger/30 rounded-xl px-4 py-3">
          <span className="text-danger text-sm">{error}</span>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-24">
          <span className="text-text-muted text-sm">Cargando portafolio...</span>
        </div>
      ) : (
        <>
          <PortfolioToolbar
            sortBy={sortBy}
            onSortChange={setSortBy}
            view={view}
            onViewChange={setView}
          />

          {view === "list" && (
            <PortfolioTable holdings={sorted} sortBy={sortBy} pricesAvailable={pricesAvailable} />
          )}

          {view === "bars" && (
            <div className="bg-bg-secondary rounded-2xl p-6">
              <p className="text-xs uppercase tracking-wide text-text-muted mb-4">
                Valor total por posición (USD)
              </p>
              <PortfolioBarChart holdings={sorted} />
            </div>
          )}

          {view === "detail" && (
            <div className="bg-bg-secondary rounded-2xl p-6">
              <PortfolioDetailView holdings={sorted} />
            </div>
          )}
        </>
      )}
    </div>
  );
}
