import { cn } from "../../../lib/cn";
import { OrderStatusBadge } from "./OrderStatusBadge";
import type { OrderHistoryItem } from "../../dashboard/services/tradingService";

interface OrderRowProps {
  order: OrderHistoryItem;
  onCancel?: (orderId: string) => void;
}

const fmt = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("es-CO", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

export function OrderRow({ order, onCancel }: OrderRowProps) {
  const isBuy = order.side === "buy";
  const canCancel = order.status === "PENDING_QUEUE" && onCancel != null;

  return (
    <tr className="border-b border-border-subtle/40 last:border-0 hover:bg-bg-tertiary/30 transition-colors duration-100">
      <td className="py-4 pr-4 text-sm text-text-secondary tabular-nums whitespace-nowrap">
        {formatDate(order.createdAt)}
      </td>
      <td className="py-4 pr-4 text-sm font-bold text-text-secondary">
        {order.symbol}
      </td>
      <td className="py-4 pr-4 text-sm text-text-secondary">{order.orderType}</td>
      <td className={cn("py-4 pr-4 text-sm font-medium", isBuy ? "text-success" : "text-danger")}>
        {isBuy ? "Compra" : "Venta"}
      </td>
      <td className="py-4 pr-4 text-sm tabular-nums text-text-secondary">
        {order.quantity}
      </td>
      <td className="py-4 pr-4 text-sm tabular-nums text-text-secondary">
        {order.executedPrice != null ? `$${fmt.format(order.executedPrice)}` : "—"}
      </td>
      <td className="py-4 pr-4">
        <OrderStatusBadge status={order.status} />
      </td>
      <td className="py-4">
        {canCancel && (
          <button
            type="button"
            onClick={() => onCancel(order.orderId)}
            className="text-xs text-danger hover:text-danger/80 border border-danger/40 hover:border-danger/70 rounded-lg px-2.5 py-1 transition-colors"
          >
            Cancelar
          </button>
        )}
      </td>
    </tr>
  );
}
