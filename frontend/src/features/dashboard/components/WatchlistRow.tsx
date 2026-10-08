import { cn } from "../../../lib/cn";
import { generateSparkline } from "../data/mockMarket";
import { useSelectedSymbol } from "../hooks/useSelectedSymbol";

interface WatchlistRowProps {
  readonly symbol: string;
  readonly name: string;
  readonly market: string;
  readonly price: number | null;
  readonly changePct: number | null;
  readonly onAdd?: (symbol: string) => void;
  readonly onRemove?: (symbol: string) => void;
  readonly inWatchlist?: boolean;
}

function getChangeClass(changePct: number | null, positive: boolean): string {
  if (changePct === null) return "text-text-muted";
  return positive ? "text-success" : "text-danger";
}

function formatChange(changePct: number | null, positive: boolean): string {
  if (changePct === null) return "—";
  const sign = positive ? "+" : "";
  return `${sign}${changePct.toFixed(1)}%`;
}

function Sparkline({ data, positive }: { data: number[]; positive: boolean }) {
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  const W = 40;
  const H = 16;
  const points = data
    .map((v, i) => {
      const x = (i / (data.length - 1)) * W;
      const y = H - ((v - min) / range) * (H - 2) - 1;
      return `${x},${y}`;
    })
    .join(" ");
  return (
    <svg width={W} height={H} className="shrink-0">
      <polyline
        points={points}
        fill="none"
        stroke={positive ? "#10B981" : "#EF4444"}
        strokeWidth={1.5}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function WatchlistRow({
  symbol,
  market,
  price,
  changePct,
  onAdd,
  onRemove,
  inWatchlist = false,
}: WatchlistRowProps) {
  const { symbol: selected, setSymbol } = useSelectedSymbol();
  const isSelected = selected === symbol;
  const positive = (changePct ?? 0) >= 0;
  const sparkData = generateSparkline(symbol);

  return (
    <div
      className={cn(
        "w-full flex items-center justify-between gap-2 py-2 px-2 rounded-lg transition-colors duration-100",
        isSelected
          ? "bg-bg-tertiary border-l-2 border-gold pl-[6px]"
          : "border-l-2 border-transparent pl-[6px] hover:bg-bg-tertiary/40"
      )}
    >
      <button
        type="button"
        onClick={() => setSymbol(symbol)}
        className="flex flex-col items-start min-w-0 flex-1 text-left"
      >
        <span className="text-sm font-bold text-text-secondary">{symbol}</span>
        <span className="text-[10px] uppercase text-text-subtle">{market}</span>
      </button>

      <span className="text-sm tabular-nums text-text-secondary">
        {price !== null && price !== undefined ? `$${price.toFixed(2)}` : "—"}
      </span>

      <div className="flex items-center gap-1">
        <span className={cn("text-xs tabular-nums", getChangeClass(changePct, positive))}>
          {formatChange(changePct, positive)}
        </span>
        <Sparkline data={sparkData} positive={positive} />
      </div>

      {(onAdd || onRemove) && (
        <button
          type="button"
          onClick={() => (inWatchlist ? onRemove?.(symbol) : onAdd?.(symbol))}
          className={cn(
            "text-xs px-1.5 py-0.5 rounded transition-colors",
            inWatchlist
              ? "text-danger hover:bg-danger/10"
              : "text-gold hover:bg-gold-subtle"
          )}
          aria-label={inWatchlist ? `Quitar ${symbol}` : `Agregar ${symbol}`}
        >
          {inWatchlist ? "−" : "+"}
        </button>
      )}
    </div>
  );
}
