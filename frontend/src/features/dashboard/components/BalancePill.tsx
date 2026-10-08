import { useTradingStore } from "../store/tradingStore";

export function BalancePill() {
  const balance = useTradingStore((s) => s.balance);

  const formatted =
    balance !== null
      ? `$${balance.toLocaleString("en-US", { minimumFractionDigits: 2 })}`
      : "–";

  return (
    <div className="flex items-center gap-2 bg-bg-secondary border border-border-subtle rounded-full px-5 py-2">
      <span className="text-xs text-text-muted">Saldo disponible</span>
      <span className="text-lg font-bold text-gold tabular-nums">{formatted}</span>
      <span className="text-xs text-text-muted">USD</span>
    </div>
  );
}
