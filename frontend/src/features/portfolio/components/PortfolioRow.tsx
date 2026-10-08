import { cn } from "../../../lib/cn";
import type { Holding } from "../data/mockHoldings";

const fmt = new Intl.NumberFormat("en-US", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

interface PortfolioRowProps {
  holding: Holding;
}

export function PortfolioRow({ holding }: PortfolioRowProps) {
  const { symbol, quantity, currentPrice, totalValue, gainDollars, gainPct } = holding;
  const isPositive = gainDollars != null && gainDollars > 0;
  const isNegative = gainDollars != null && gainDollars < 0;

  const gpColor = cn(
    isPositive && "text-success",
    isNegative && "text-danger",
    !isPositive && !isNegative && "text-text-muted"
  );

  function gpPrefix(n: number) {
    return n > 0 ? "+" : "";
  }

  return (
    <tr className="border-b border-border-subtle/40 last:border-0 hover:bg-bg-tertiary/30 transition-colors duration-100">
      <td className="py-4 pr-4">
        <span className="text-sm font-bold text-text-secondary">{symbol}</span>
      </td>
      <td className="py-4 pr-4 tabular-nums text-sm text-text-secondary">
        {quantity}
      </td>
      <td className="py-4 pr-4 tabular-nums text-sm text-text-secondary">
        {currentPrice != null ? `$${fmt.format(currentPrice)}` : "—"}
      </td>
      <td className="py-4 pr-4 tabular-nums text-sm text-text-secondary">
        {totalValue != null ? `$${fmt.format(totalValue)}` : "—"}
      </td>
      <td className={cn("py-4 pr-4 tabular-nums text-sm", gpColor)}>
        {gainDollars != null
          ? `${gpPrefix(gainDollars)}$${fmt.format(Math.abs(gainDollars))}`
          : "—"}
      </td>
      <td className={cn("py-4 tabular-nums text-sm", gpColor)}>
        {gainPct != null
          ? `${gpPrefix(gainPct)}${Math.abs(gainPct).toFixed(2)}%`
          : "—"}
      </td>
    </tr>
  );
}
