import { useEffect, useState } from "react";
import { PageHeader } from "../../../components/layout/PageHeader";
import { OrdersFilters, type OrderFilters } from "../components/OrdersFilters";
import { OrdersTable } from "../components/OrdersTable";
import { useSessionStore } from "../../../store/sessionStore";
import { cancelOrder, getOrders, type OrderHistoryItem } from "../../dashboard/services/tradingService";

const PAGE_SIZE = 20;

export function OrdersPage() {
  const token = useSessionStore((s) => s.token);

  const [filters, setFilters] = useState<OrderFilters>({
    symbol: "",
    status: "",
    from: "",
    to: "",
  });
  const [page, setPage] = useState(0);
  const [orders, setOrders] = useState<OrderHistoryItem[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cancelError, setCancelError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    setLoading(true);
    setError(null);
    getOrders(
      token,
      {
        symbol: filters.symbol || undefined,
        status: filters.status || undefined,
        startDate: filters.from || undefined,
        endDate: filters.to || undefined,
      },
      page,
      PAGE_SIZE
    )
      .then((data) => {
        setOrders(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch(() => setError("No se pudo cargar el historial. Intenta de nuevo."))
      .finally(() => setLoading(false));
  }, [token, filters, page]);

  function handleFiltersChange(next: OrderFilters) {
    setFilters(next);
    setPage(0);
  }

  async function handleCancel(orderId: string) {
    if (!token) return;
    setCancelError(null);
    try {
      await cancelOrder(orderId, token);
      setOrders((prev) =>
        prev.map((o) => (o.orderId === orderId ? { ...o, status: "CANCELLED" } : o))
      );
    } catch (err) {
      setCancelError(err instanceof Error ? err.message : "No se pudo cancelar la orden");
    }
  }

  return (
    <>
      <PageHeader
        title="Órdenes"
        subtitle="Historial de tus órdenes de compra y venta"
      />

      <OrdersFilters filters={filters} onChange={handleFiltersChange} />

      {error && (
        <div className="mb-4 bg-danger/10 border border-danger/30 rounded-xl px-4 py-3 text-danger text-sm">
          {error}
        </div>
      )}

      {cancelError && (
        <div className="mb-4 bg-danger/10 border border-danger/30 rounded-xl px-4 py-3 text-danger text-sm">
          {cancelError}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <span className="text-text-muted text-sm">Cargando órdenes...</span>
        </div>
      ) : (
        <OrdersTable orders={orders} onCancel={handleCancel} />
      )}

      {!loading && totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 px-2">
          <span className="text-xs text-text-muted">
            {totalElements} órdenes en total
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
    </>
  );
}
