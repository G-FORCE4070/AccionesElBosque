import { useEffect, useState } from "react";
import { PageHeader } from "../../../components/layout/PageHeader";
import { TransactionsFilters, type TxFilters } from "../components/TransactionsFilters";
import { TransactionsTable } from "../components/TransactionsTable";
import { useSessionStore } from "../../../store/sessionStore";
import { getTransactions, type TransactionItem } from "../../dashboard/services/tradingService";

const PAGE_SIZE = 20;
const fmt = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export function TransactionsPage() {
  const token = useSessionStore((s) => s.token);

  const [filters, setFilters] = useState<TxFilters>({
    side: "all",
    symbol: "",
    from: "",
    to: "",
  });
  const [page, setPage] = useState(0);
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalCommission, setTotalCommission] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    setLoading(true);
    setError(null);
    getTransactions(
      token,
      {
        type: filters.side !== "all" ? filters.side : undefined,
        symbol: filters.symbol || undefined,
        startDate: filters.from || undefined,
        endDate: filters.to || undefined,
      },
      page,
      PAGE_SIZE
    )
      .then((data) => {
        setTransactions(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
        setTotalCommission(data.totalCommission);
      })
      .catch(() => setError("No se pudo cargar el historial. Intenta de nuevo."))
      .finally(() => setLoading(false));
  }, [token, filters, page]);

  function handleFiltersChange(next: TxFilters) {
    setFilters(next);
    setPage(0);
  }

  return (
    <div>
      <PageHeader
        title="Transacciones"
        subtitle="Historial de compras y ventas ejecutadas con detalle de comisiones"
      />

      <TransactionsFilters filters={filters} onChange={handleFiltersChange} />

      {error && (
        <div className="mb-4 bg-danger/10 border border-danger/30 rounded-xl px-4 py-3 text-danger text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <span className="text-text-muted text-sm">Cargando transacciones...</span>
        </div>
      ) : (
        <>
          <TransactionsTable transactions={transactions} />

          {transactions.length > 0 && (
            <div className="mt-3 px-2 text-sm text-text-muted">
              Comisión total en esta página:{" "}
              <span className="font-semibold text-text-secondary">
                ${fmt.format(totalCommission)}
              </span>
            </div>
          )}
        </>
      )}

      {!loading && totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 px-2">
          <span className="text-xs text-text-muted">
            {totalElements} transacciones en total
          </span>
          <div className="flex items-center gap-3">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="px-4 py-1.5 rounded-lg border border-border-default text-sm text-text-secondary hover:border-border-focus transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Anterior
            </button>
            <span className="text-xs text-text-muted tabular-nums">
              {page + 1} / {totalPages}
            </span>
            <button
              type="button"
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-4 py-1.5 rounded-lg border border-border-default text-sm text-text-secondary hover:border-border-focus transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Siguiente
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
