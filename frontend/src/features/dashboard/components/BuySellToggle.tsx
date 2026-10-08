import { cn } from "../../../lib/cn";
import type { OrderSide } from "../hooks/useOrderDraft";

interface BuySellToggleProps {
  value: OrderSide;
  onChange: (side: OrderSide) => void;
}

export function BuySellToggle({ value, onChange }: BuySellToggleProps) {
  return (
    <div className="flex gap-2">
      <button
        type="button"
        onClick={() => onChange("buy")}
        className={cn(
          "flex-1 py-2.5 rounded-xl font-semibold text-sm transition-colors duration-150",
          value === "buy"
            ? "bg-success text-white"
            : "bg-bg-tertiary text-text-muted hover:bg-bg-tertiary/80"
        )}
      >
        Comprar
      </button>
      <button
        type="button"
        onClick={() => onChange("sell")}
        className={cn(
          "flex-1 py-2.5 rounded-xl font-semibold text-sm transition-colors duration-150",
          value === "sell"
            ? "bg-danger text-white"
            : "bg-bg-tertiary text-text-muted hover:bg-bg-tertiary/80"
        )}
      >
        Vender
      </button>
    </div>
  );
}
