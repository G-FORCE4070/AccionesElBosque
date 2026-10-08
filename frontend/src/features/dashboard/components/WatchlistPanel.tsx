import { Link } from "react-router-dom";
import { Card } from "../../../components/ui/Card";
import { WatchlistGroup, type StockEntry } from "./WatchlistGroup";
import { useMarketStocks } from "../hooks/useMarketStocks";
import { useWatchlist } from "../hooks/useWatchlist";
import { useSessionStore } from "../../../store/sessionStore";
import { useProfileStore } from "../../profile/store/profileStore";

const SUGGESTIONS = ["AAPL", "MSFT", "NVDA", "GOOGL", "TSLA", "META", "AMZN", "JPM", "JNJ", "XOM"];
const SKELETON_ROWS = ["sk-1", "sk-2", "sk-3", "sk-4", "sk-5"];

function getWatchlistEmptyMessage(isPremium: boolean, symbolCount: number): string | undefined {
  if (!isPremium) return "Función exclusiva para usuarios Premium";
  if (symbolCount === 0) return "Agrega símbolos desde el mercado";
  return undefined;
}

function StockSkeleton() {
  return (
    <div className="flex flex-col gap-0.5">
      {SKELETON_ROWS.map((key) => (
        <div key={key} className="h-[42px] rounded-lg bg-bg-tertiary/40 animate-pulse mx-1" />
      ))}
    </div>
  );
}

export function WatchlistPanel() {
  const plan = useSessionStore((s) => s.user?.plan ?? "basic");
  const isPremium = plan.toUpperCase() === "PREMIUM";
  const market = useProfileStore((s) => s.market);
  const sector = useProfileStore((s) => s.sector);

  const { stocks, loading, error, refetch } = useMarketStocks();
  const { symbols, prices, add, remove } = useWatchlist(isPremium);

  const watchlistStocks: StockEntry[] = symbols
    .map((sym) => prices[sym])
    .filter(Boolean)
    .map((s) => ({
      symbol: s.symbol,
      companyName: s.companyName,
      market: s.market,
      sector: s.sector,
      price: s.price,
      change24h: s.change24h,
    }));

  const marketStocks: StockEntry[] = stocks.map((s) => ({
    symbol: s.symbol,
    companyName: s.companyName,
    market: s.market,
    sector: s.sector,
    price: s.price,
    change24h: s.change24h,
  }));

  const watchlistLabel = isPremium ? `Mi Watchlist (${symbols.length})` : "Mi Watchlist";

  const marketLabel = [market ? market.toUpperCase() : "Todos", sector || null]
    .filter(Boolean)
    .join(" · ");

  let marketContent;
  if (error) {
    marketContent = (
      <div className="flex flex-col items-center gap-2 py-4">
        <p className="text-xs text-text-muted text-center">
          Datos de mercado no disponibles temporalmente
        </p>
        <button
          type="button"
          onClick={refetch}
          className="text-xs text-gold underline hover:text-gold-light"
        >
          Reintentar
        </button>
      </div>
    );
  } else if (loading) {
    marketContent = <StockSkeleton />;
  } else {
    marketContent = (
      <WatchlistGroup
        stocks={marketStocks}
        header={
          <div className="flex items-center justify-between w-full">
            <span className="text-[10px] uppercase tracking-wider text-text-subtle">
              {marketLabel}
            </span>
            <span className="text-[10px] uppercase tracking-wider text-text-subtle">
              {stocks.length} acciones
            </span>
          </div>
        }
        onAdd={isPremium ? add : undefined}
        onRemove={isPremium ? remove : undefined}
        watchlistSymbols={symbols}
      />
    );
  }

  return (
    <Card className="flex flex-col gap-4 p-4 overflow-y-auto">
      <div className="flex items-center justify-between">
        <span className="text-base font-semibold text-text-secondary">
          Listas de seguimiento
        </span>
        <Link to="/profile" className="text-xs text-gold underline hover:text-gold-light">
          Administrar
        </Link>
      </div>

      <WatchlistGroup
        stocks={isPremium ? watchlistStocks : []}
        emptyMessage={getWatchlistEmptyMessage(isPremium, symbols.length)}
        header={
          <div className="flex items-center gap-2">
            <span className="text-xs uppercase tracking-wide text-text-muted">
              {watchlistLabel}
            </span>
            <span className="bg-gold-subtle text-gold text-[10px] uppercase px-2 py-0.5 rounded-full">
              Premium
            </span>
          </div>
        }
        onRemove={isPremium ? remove : undefined}
      />

      {isPremium && symbols.length === 0 && (
        <div className="flex flex-wrap gap-1 px-2">
          {SUGGESTIONS.map((sym) => (
            <button
              key={sym}
              type="button"
              onClick={() => add(sym)}
              className="text-[10px] px-2 py-0.5 rounded-full border border-gold/40 text-gold hover:bg-gold-subtle transition-colors"
            >
              {sym}
            </button>
          ))}
        </div>
      )}

      <div className="border-t border-border-subtle/60" />

      {marketContent}
    </Card>
  );
}
