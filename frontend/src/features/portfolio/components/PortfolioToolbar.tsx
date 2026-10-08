export type SortKey = "totalValue" | "quantity" | "gainDollars" | "gainPct" | "symbol";
export type ViewMode = "list" | "bars" | "detail";

interface PortfolioToolbarProps {
  sortBy: SortKey;
  onSortChange: (key: SortKey) => void;
  view: ViewMode;
  onViewChange: (v: ViewMode) => void;
}

const sortOptions: { value: SortKey; label: string }[] = [
  { value: "totalValue", label: "Valor total" },
  { value: "quantity",   label: "Cantidad" },
  { value: "gainDollars", label: "G/P ($)" },
  { value: "gainPct",    label: "G/P (%)" },
  { value: "symbol",     label: "Símbolo" },
];

const viewOptions: { value: ViewMode; label: string }[] = [
  { value: "list",   label: "Lista" },
  { value: "bars",   label: "Gráfico de barras" },
  { value: "detail", label: "Vista detallada" },
];

const selectClass =
  "bg-bg-tertiary border border-border-default rounded-lg px-3 py-1.5 text-sm text-text-secondary focus:outline-none focus:border-border-focus transition-colors duration-150";

export function PortfolioToolbar({ sortBy, onSortChange, view, onViewChange }: PortfolioToolbarProps) {
  return (
    <div className="bg-bg-secondary rounded-2xl p-4 mb-4 flex items-center gap-6">
      <div className="flex items-center">
        <span className="text-xs text-text-muted mr-2">Vista:</span>
        <select className={selectClass} value={view} onChange={(e) => onViewChange(e.target.value as ViewMode)}>
          {viewOptions.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
      </div>

      <div className="flex items-center">
        <span className="text-xs text-text-muted mr-2">Ordenar por:</span>
        <select className={selectClass} value={sortBy} onChange={(e) => onSortChange(e.target.value as SortKey)}>
          {sortOptions.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
      </div>
    </div>
  );
}
