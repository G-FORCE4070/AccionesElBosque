import type { ReactNode } from "react";
import { WatchlistRow } from "./WatchlistRow";

export interface StockEntry {
  symbol: string;
  companyName: string;
  market: string;
  sector: string;
  price: number | null;
  change24h: number | null;
}

interface WatchlistGroupProps {
  readonly stocks: StockEntry[];
  readonly header: ReactNode;
  readonly emptyMessage?: string;
  readonly onAdd?: (symbol: string) => void;
  readonly onRemove?: (symbol: string) => void;
  readonly watchlistSymbols?: string[];
}

export function WatchlistGroup({
  stocks,
  header,
  emptyMessage,
  onAdd,
  onRemove,
  watchlistSymbols = [],
}: WatchlistGroupProps) {
  return (
    <div className="flex flex-col gap-0.5">
      <div className="flex items-center justify-between mb-1">{header}</div>
      <div className="flex items-center justify-between px-2 mb-1">
        <span className="text-[10px] uppercase tracking-wider text-text-subtle">Símbolo</span>
        <span className="text-[10px] uppercase tracking-wider text-text-subtle">Precio</span>
        <span className="text-[10px] uppercase tracking-wider text-text-subtle">Cambio</span>
      </div>
      {stocks.length === 0 && emptyMessage ? (
        <p className="text-xs text-text-muted px-2 py-3">{emptyMessage}</p>
      ) : (
        stocks.map((s) => (
          <WatchlistRow
            key={s.symbol}
            symbol={s.symbol}
            name={s.companyName}
            market={s.market}
            price={s.price}
            changePct={s.change24h}
            onAdd={onAdd}
            onRemove={onRemove}
            inWatchlist={watchlistSymbols.includes(s.symbol)}
          />
        ))
      )}
    </div>
  );
}
