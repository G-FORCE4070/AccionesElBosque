import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { Card } from "../../../components/ui/Card";
import { Input } from "../../../components/ui/Input";
import { Select } from "../../../components/ui/Select";
import { BuySellToggle } from "./BuySellToggle";
import { OrderSummary } from "./OrderSummary";
import { OrderConfirmModal } from "./OrderConfirmModal";
import { useSelectedSymbol } from "../hooks/useSelectedSymbol";
import { useOrderDraft } from "../hooks/useOrderDraft";
import { getStocks, type StockData } from "../services/marketsService";
import { useSessionStore } from "../../../store/sessionStore";
import { cn } from "../../../lib/cn";

export function NewOrderPanel() {
  const symbol = useSelectedSymbol((s) => s.symbol);
  const token = useSessionStore((s) => s.token ?? "");
  const [allStocks, setAllStocks] = useState<StockData[]>([]);

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    const load = () => {
      getStocks(token).then((data) => { if (!cancelled) setAllStocks(data); }).catch(() => {});
    };
    load();
    const id = setInterval(load, 30_000);
    return () => { cancelled = true; clearInterval(id); };
  }, [token]);

  const currentStock = allStocks.find((s) => s.symbol === symbol);
  const currentPrice = currentStock?.price ?? 0;
  const stockName = currentStock?.companyName ?? symbol;

  const { draft, set, computed, isValid } = useOrderDraft(symbol, currentPrice);
  const [modalOpen, setModalOpen] = useState(false);

  const stockOptions = allStocks.map((s) => ({
    value: s.symbol,
    label: `${s.symbol} - ${s.companyName}`,
  }));

  const actionLabel = draft.side === "buy" ? `Comprar ${symbol}` : `Vender ${symbol}`;

  return (
    <>
      <Card className="flex flex-col gap-4 p-4 overflow-y-auto">
        <div className="flex items-center justify-between">
          <span className="text-base font-semibold text-text-secondary">Nueva Orden</span>
          <Link to="/orders" className="text-xs text-gold underline hover:text-gold-light">
            Historial
          </Link>
        </div>

        <BuySellToggle value={draft.side} onChange={(side) => set("side", side)} />

        <Select
          label="Tipo de orden"
          options={[{ value: "market", label: "Market" }]}
          value="market"
          disabled
        />

        <Select
          label="Acción"
          options={stockOptions}
          value={symbol}
          onChange={(v) => useSelectedSymbol.getState().setSymbol(v)}
        />

        <Input
          label="Cantidad"
          type="number"
          min={1}
          step={1}
          value={draft.quantity}
          onChange={(e) => set("quantity", parseInt(e.target.value, 10) || 1)}
        />

        <OrderSummary computed={computed} side={draft.side} />

        <button
          type="button"
          disabled={!isValid}
          onClick={() => setModalOpen(true)}
          className={cn(
            "w-full py-3.5 rounded-xl font-bold text-sm text-white transition-colors duration-150",
            "disabled:opacity-50 disabled:cursor-not-allowed",
            draft.side === "buy"
              ? "bg-success hover:bg-success/90"
              : "bg-danger hover:bg-danger/90"
          )}
        >
          {actionLabel}
        </button>
      </Card>

      <OrderConfirmModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        draft={draft}
        computed={computed}
        stockName={stockName}
      />
    </>
  );
}
