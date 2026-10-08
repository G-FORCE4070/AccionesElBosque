export interface TxFilters {
  side: "all" | "buy" | "sell";
  symbol: string;
  from: string;
  to: string;
}

interface TransactionsFiltersProps {
  filters: TxFilters;
  onChange: (f: TxFilters) => void;
}

const inputClass =
  "bg-bg-tertiary border border-border-default rounded-lg px-3 py-1.5 text-sm text-text-secondary focus:outline-none focus:border-border-focus transition-colors duration-150";

export function TransactionsFilters({ filters, onChange }: TransactionsFiltersProps) {
  function set<K extends keyof TxFilters>(k: K, v: TxFilters[K]) {
    onChange({ ...filters, [k]: v });
  }

  return (
    <div className="bg-bg-secondary rounded-2xl p-4 mb-4 flex flex-wrap items-center gap-4">
      <div className="flex items-center">
        <span className="text-xs text-text-muted mr-2">Tipo:</span>
        <select
          className={inputClass}
          value={filters.side}
          onChange={(e) => set("side", e.target.value as TxFilters["side"])}
        >
          <option value="all">Todos</option>
          <option value="buy">Compra</option>
          <option value="sell">Venta</option>
        </select>
      </div>

      <div className="flex items-center">
        <span className="text-xs text-text-muted mr-2">Símbolo:</span>
        <input
          className={inputClass}
          placeholder="Ej: AAPL"
          value={filters.symbol}
          onChange={(e) => set("symbol", e.target.value)}
        />
      </div>

      <div className="flex items-center">
        <span className="text-xs text-text-muted mr-2">Desde:</span>
        <input
          type="date"
          className={inputClass}
          value={filters.from}
          onChange={(e) => set("from", e.target.value)}
        />
      </div>

      <div className="flex items-center">
        <span className="text-xs text-text-muted mr-2">Hasta:</span>
        <input
          type="date"
          className={inputClass}
          value={filters.to}
          onChange={(e) => set("to", e.target.value)}
        />
      </div>
    </div>
  );
}
