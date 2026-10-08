import type { OrderComputed, OrderSide } from "../hooks/useOrderDraft";

interface OrderSummaryProps {
  computed: OrderComputed;
  side: OrderSide;
}

function fmt(n: number) {
  return `$${n.toFixed(2)}`;
}

export function OrderSummary({ computed, side }: OrderSummaryProps) {
  const { subtotal, commission, total } = computed;
  const sign = side === "buy" ? "+" : "-";

  return (
    <div className="bg-bg-tertiary rounded-xl p-4 flex flex-col gap-2">
      <Row label="Subtotal" value={fmt(subtotal)} />
      <Row label="Comisión (2%)" value={fmt(commission)} />
      <div className="border-t border-border-subtle/60 pt-2 mt-1">
        <div className="flex items-center justify-between">
          <span className="text-sm font-bold text-text-secondary">Total</span>
          <span
            className={`text-base tabular-nums font-bold ${
              side === "buy" ? "text-success" : "text-danger"
            }`}
          >
            {sign}
            {fmt(total)}
          </span>
        </div>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm text-text-secondary">{label}</span>
      <span className="text-sm tabular-nums text-text-primary font-medium">{value}</span>
    </div>
  );
}
