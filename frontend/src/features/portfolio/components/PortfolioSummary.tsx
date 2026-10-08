import type { Holding } from "../data/mockHoldings";

const fmt = new Intl.NumberFormat("en-US", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

interface PortfolioSummaryProps {
  holdings: Holding[];
  pricesAvailable: boolean;
}

export function PortfolioSummary({ holdings, pricesAvailable }: PortfolioSummaryProps) {
  if (!pricesAvailable) {
    return (
      <div className="border-t border-border-subtle/40 pt-6 mt-2">
        <p className="text-sm text-text-muted text-center">Resumen no disponible (precios sin conexión)</p>
      </div>
    );
  }

  const currentValue = holdings.reduce((acc, h) => acc + (h.totalValue ?? 0), 0);
  const totalGain = holdings.reduce((acc, h) => acc + (h.gainDollars ?? 0), 0);
  const investmentTotal = currentValue - totalGain;
  const gainPct = investmentTotal > 0 ? (totalGain / investmentTotal) * 100 : 0;

  const gainColor = totalGain >= 0 ? "text-success" : "text-danger";
  const gainPrefix = totalGain >= 0 ? "+" : "";

  return (
    <div className="border-t border-border-subtle/40 pt-6 mt-2">
      <SummaryRow label="Inversión total" value={`$${fmt.format(investmentTotal)}`} />
      <SummaryRow label="Valor actual" value={`$${fmt.format(currentValue)}`} />
      <div className="flex justify-between items-center py-2">
        <span className="text-sm text-text-muted">Ganancia/Pérdida total</span>
        <span className={`text-base tabular-nums font-medium ${gainColor}`}>
          {gainPrefix}${fmt.format(Math.abs(totalGain))}{" "}
          <span className="text-sm">
            ({gainPrefix}{Math.abs(gainPct).toFixed(2)}%)
          </span>
        </span>
      </div>
    </div>
  );
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between items-center py-2">
      <span className="text-sm text-text-muted">{label}</span>
      <span className="text-base tabular-nums font-medium text-text-primary">{value}</span>
    </div>
  );
}
