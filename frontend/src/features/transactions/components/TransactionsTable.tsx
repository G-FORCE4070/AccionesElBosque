import { Search } from "lucide-react";
import { TransactionRow } from "./TransactionRow";
import type { TransactionItem } from "../../dashboard/services/tradingService";

const COLS = ["FECHA", "TIPO", "SÍMBOLO", "CANTIDAD", "PRECIO UNIT.", "SUBTOTAL", "COMISIÓN", "TOTAL"];

export function TransactionsTable({ transactions }: { transactions: TransactionItem[] }) {
  return (
    <div className="bg-bg-secondary rounded-2xl p-6">
      {transactions.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-14 gap-3">
          <Search size={32} className="text-text-muted" />
          <p className="text-base font-semibold text-text-secondary">Sin resultados</p>
          <p className="text-sm text-text-muted">
            Prueba ajustando los filtros para ver más transacciones.
          </p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[820px]">
            <thead>
              <tr>
                {COLS.map((col) => (
                  <th
                    key={col}
                    className={`text-xs uppercase tracking-wide text-text-muted py-3 pr-4 last:pr-0 whitespace-nowrap ${
                      col === "TOTAL" ? "text-right" : "text-left"
                    }`}
                  >
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {transactions.map((tx) => (
                <TransactionRow key={tx.transactionId} tx={tx} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
