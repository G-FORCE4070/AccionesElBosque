import { OrderRow } from "./OrderRow";
import type { OrderHistoryItem } from "../../dashboard/services/tradingService";

const COLUMNS = ["FECHA", "SÍMBOLO", "TIPO", "LADO", "CANTIDAD", "PRECIO EJEC.", "ESTADO", ""];

interface OrdersTableProps {
  orders: OrderHistoryItem[];
  onCancel?: (orderId: string) => void;
}

export function OrdersTable({ orders, onCancel }: OrdersTableProps) {
  return (
    <div className="bg-bg-secondary rounded-2xl p-6">
      {orders.length === 0 ? (
        <p className="text-center text-sm text-text-muted py-10">
          No se encontraron órdenes con los filtros aplicados.
        </p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[680px]">
            <thead>
              <tr>
                {COLUMNS.map((col, i) => (
                  <th
                    key={i}
                    className="text-xs uppercase tracking-wide text-text-muted py-3 text-left pr-4 last:pr-0 whitespace-nowrap"
                  >
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {orders.map((o) => (
                <OrderRow key={o.orderId} order={o} onCancel={onCancel} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
