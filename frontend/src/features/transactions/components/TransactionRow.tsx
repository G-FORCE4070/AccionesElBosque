import { cn } from "../../../lib/cn";
import { CommissionBreakdown } from "./CommissionBreakdown";
import type { TransactionItem } from "../../dashboard/services/tradingService";

const fmt = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("es-CO", {
    dateStyle: "short",
    timeStyle: "short",
  });
}

export function TransactionRow({ tx }: { tx: TransactionItem }) {
  const isBuy = tx.type === "buy";
  const totalAbs = Math.abs(tx.total);
  const totalPrefix = isBuy ? "-" : "+";
  const commissionPct = tx.subtotal > 0 ? (tx.commission / tx.subtotal) * 100 : 0;

  return (
    <tr className="border-b border-border-subtle/40 last:border-0 hover:bg-bg-tertiary/30 transition-colors duration-100">
      <td className="py-5 pr-4 text-sm text-text-secondary tabular-nums whitespace-nowrap">
        {formatDate(tx.date)}
      </td>
      <td className="py-5 pr-4">
        <span
          className={cn(
            "inline-flex items-center px-3 py-1 rounded-full text-xs font-medium",
            isBuy ? "bg-success/10 text-success" : "bg-danger/10 text-danger"
          )}
        >
          {isBuy ? "Compra" : "Venta"}
        </span>
      </td>
      <td className="py-5 pr-4 text-sm font-bold text-text-secondary">{tx.symbol}</td>
      <td className="py-5 pr-4 text-sm tabular-nums text-text-secondary">{tx.quantity}</td>
      <td className="py-5 pr-4 text-sm tabular-nums text-text-secondary">
        ${fmt.format(tx.unitPrice)}
      </td>
      <td className="py-5 pr-4 text-sm tabular-nums text-text-secondary">
        ${fmt.format(tx.subtotal)}
      </td>
      <td className="py-5 pr-4">
        <CommissionBreakdown
          total={tx.commission}
          pct={commissionPct}
        />
      </td>
      <td className="py-5 text-right">
        <span
          className={cn(
            "text-base font-semibold tabular-nums",
            isBuy ? "text-danger" : "text-success"
          )}
        >
          {totalPrefix}${fmt.format(totalAbs)}
        </span>
      </td>
    </tr>
  );
}
