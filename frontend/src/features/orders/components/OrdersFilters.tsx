const inputClass =
  "bg-bg-tertiary border border-border-default rounded-lg px-3 py-1.5 text-sm text-text-secondary focus:outline-none focus:border-border-focus transition-colors duration-150";

const STATUS_OPTIONS = [
  { value: "",             label: "Todos" },
  { value: "PENDING_QUEUE", label: "En Cola" },
  { value: "SENT",          label: "Enviada" },
  { value: "EXECUTED",      label: "Ejecutada" },
  { value: "REJECTED",      label: "Rechazada" },
  { value: "FAILED",        label: "Fallida" },
];

export interface OrderFilters {
  symbol: string;
  status: string;
  from: string;
  to: string;
}

interface OrdersFiltersProps {
  filters: OrderFilters;
  onChange: (filters: OrderFilters) => void;
}

export function OrdersFilters({ filters, onChange }: OrdersFiltersProps) {
  function set<K extends keyof OrderFilters>(k: K, v: OrderFilters[K]) {
    onChange({ ...filters, [k]: v });
  }

  return (
    <div className="bg-bg-secondary rounded-2xl p-4 mb-4 flex flex-wrap items-center gap-4">
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
        <span className="text-xs text-text-muted mr-2">Estado:</span>
        <select
          className={inputClass}
          value={filters.status}
          onChange={(e) => set("status", e.target.value)}
        >
          {STATUS_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
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
