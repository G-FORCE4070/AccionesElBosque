import { cn } from "../../../lib/cn";
import { PortfolioRow } from "./PortfolioRow";
import { PortfolioSummary } from "./PortfolioSummary";
import type { Holding } from "../data/mockHoldings";
import type { SortKey } from "./PortfolioToolbar";

interface Col {
  key: SortKey | null;
  label: string;
}

const COLUMNS: Col[] = [
  { key: "symbol",     label: "Símbolo" },
  { key: "quantity",   label: "Cantidad" },
  { key: null,         label: "Precio actual" },
  { key: "totalValue", label: "Valor total" },
  { key: "gainDollars", label: "G/P ($)" },
  { key: "gainPct",    label: "G/P (%)" },
];

interface PortfolioTableProps {
  holdings: Holding[];
  sortBy: SortKey;
  pricesAvailable: boolean;
}

export function PortfolioTable({ holdings, sortBy, pricesAvailable }: PortfolioTableProps) {
  return (
    <div className="bg-bg-secondary rounded-2xl p-6">
      {holdings.length === 0 ? (
        <EmptyState />
      ) : (
        <>
          <table className="w-full">
            <thead>
              <tr>
                {COLUMNS.map((col) => (
                  <th
                    key={col.label}
                    className="text-xs uppercase tracking-wide text-text-muted py-3 text-left pr-4 last:pr-0"
                  >
                    <span className="flex items-center gap-1">
                      {col.label}
                      {col.key && (
                        <span
                          className={cn(
                            "text-[10px]",
                            col.key === sortBy ? "text-gold" : "text-text-subtle"
                          )}
                        >
                          {col.key === sortBy ? "▼" : "↕"}
                        </span>
                      )}
                    </span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {holdings.map((h) => (
                <PortfolioRow key={h.symbol} holding={h} />
              ))}
            </tbody>
          </table>
          <PortfolioSummary holdings={holdings} pricesAvailable={pricesAvailable} />
        </>
      )}
    </div>
  );
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center py-16 gap-4">
      <svg
        xmlns="http://www.w3.org/2000/svg"
        width={48}
        height={48}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth={1.5}
        className="text-text-muted"
      >
        <path d="M21 12V7H5a2 2 0 0 1 0-4h14v4" />
        <path d="M3 5v14a2 2 0 0 0 2 2h16v-5" />
        <path d="M18 12a2 2 0 0 0 0 4h4v-4Z" />
      </svg>
      <div className="text-center">
        <p className="text-base font-semibold text-text-secondary mb-1">
          Tu portafolio está vacío
        </p>
        <p className="text-sm text-text-muted">
          Comienza a operar desde la Terminal de Trading.
        </p>
      </div>
      <a
        href="/dashboard"
        className="mt-2 inline-flex items-center justify-center h-11 px-6 rounded-xl bg-gold text-bg-primary font-medium text-base hover:bg-gold-light transition-colors duration-150"
      >
        Ir al Dashboard
      </a>
    </div>
  );
}
