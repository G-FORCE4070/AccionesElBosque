import { useState } from "react";
import { createPortal } from "react-dom";
import { Check, X, AlertCircle, Clock } from "lucide-react";
import { OrderSummary } from "./OrderSummary";
import type { OrderDraft, OrderComputed } from "../hooks/useOrderDraft";
import { placeOrder, MarketClosedError, type OrderResult, type QueuedOrderResult } from "../services/tradingService";
import { useTradingStore } from "../store/tradingStore";
import { useSessionStore } from "../../../store/sessionStore";
import { cn } from "../../../lib/cn";

interface OrderConfirmModalProps {
  open: boolean;
  onClose: () => void;
  draft: OrderDraft;
  computed: OrderComputed;
  stockName: string;
}

type ModalState = "idle" | "loading" | "success" | "error" | "queued-confirm" | "queued-loading" | "queued-success";

export function OrderConfirmModal({
  open,
  onClose,
  draft,
  computed,
  stockName,
}: OrderConfirmModalProps) {
  const [state, setState] = useState<ModalState>("idle");
  const [orderId, setOrderId] = useState("");
  const [errorMsg, setErrorMsg] = useState("");
  const [toast, setToast] = useState(false);

  const token = useSessionStore((s) => s.token ?? "");
  const updateBalance = useTradingStore((s) => s.updateBalance);

  if (!open) return null;

  const isBuy = draft.side === "buy";
  const title = `Confirmar orden de ${isBuy ? "compra" : "venta"}`;
  const actionColor = isBuy ? "bg-success hover:bg-success/90" : "bg-danger hover:bg-danger/90";

  async function handleConfirm() {
    setState("loading");
    try {
      const result = await placeOrder(draft.side, draft.symbol, draft.quantity, token);
      const executed = result as OrderResult;
      setOrderId(executed.orderId);
      updateBalance(executed.newBalance);
      setState("success");
      setTimeout(() => {
        setState("idle");
        setToast(true);
        onClose();
        setTimeout(() => setToast(false), 4000);
      }, 2000);
    } catch (err) {
      if (err instanceof MarketClosedError) {
        setState("queued-confirm");
      } else {
        setErrorMsg(err instanceof Error ? err.message : "Error al procesar la orden");
        setState("error");
      }
    }
  }

  async function handleEnqueue() {
    setState("queued-loading");
    try {
      const result = await placeOrder(draft.side, draft.symbol, draft.quantity, token, true);
      const queued = result as QueuedOrderResult;
      setOrderId(queued.orderId);
      setState("queued-success");
    } catch (err) {
      setErrorMsg(err instanceof Error ? err.message : "Error al encolar la orden");
      setState("error");
    }
  }

  function handleClose() {
    if (state === "loading" || state === "queued-loading") return;
    setState("idle");
    setErrorMsg("");
    onClose();
  }

  return createPortal(
    <>
      {/* Overlay */}
      <div
        className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
        role="presentation"
        onClick={handleClose}
        onKeyDown={(e) => { if (e.key === "Escape") handleClose(); }}
      >
        {/* Modal */}
        <div
          className="relative bg-bg-elevated border border-border-subtle rounded-2xl p-6 w-full max-w-[440px] shadow-2xl"
          onClick={(e) => e.stopPropagation()}
        >
          {state !== "loading" && state !== "queued-loading" && (
            <button
              type="button"
              onClick={handleClose}
              className="absolute top-4 right-4 text-text-subtle hover:text-text-secondary"
            >
              <X size={18} />
            </button>
          )}

          {state === "success" && (
            <div className="flex flex-col items-center gap-4 py-4">
              <div className="w-16 h-16 rounded-full bg-success/20 flex items-center justify-center">
                <Check size={32} className="text-success" />
              </div>
              <div className="text-center">
                <p className="font-display text-2xl text-text-primary mb-1">Orden enviada exitosamente</p>
                <p className="text-sm text-text-muted">ID: {orderId}</p>
              </div>
            </div>
          )}

          {state === "queued-success" && (
            <div className="flex flex-col items-center gap-4 py-4">
              <div className="w-16 h-16 rounded-full bg-gold/20 flex items-center justify-center">
                <Clock size={32} className="text-gold" />
              </div>
              <div className="text-center">
                <p className="font-display text-2xl text-text-primary mb-1">Orden encolada</p>
                <p className="text-sm text-text-secondary">
                  Se ejecutará automáticamente cuando el mercado abra.
                </p>
                <p className="text-xs text-text-muted mt-1">ID: {orderId}</p>
              </div>
              <button
                type="button"
                onClick={handleClose}
                className="mt-2 px-6 py-2 rounded-xl border border-border-default text-text-secondary text-sm font-medium hover:bg-bg-tertiary transition-colors"
              >
                Cerrar
              </button>
            </div>
          )}

          {state === "queued-confirm" && (
            <div className="flex flex-col gap-4">
              <div className="flex flex-col items-center gap-3 py-2">
                <div className="w-14 h-14 rounded-full bg-gold/20 flex items-center justify-center">
                  <Clock size={28} className="text-gold" />
                </div>
                <h2 className="font-display text-xl text-text-primary text-center">
                  El mercado está cerrado
                </h2>
                <p className="text-sm text-text-secondary text-center">
                  ¿Deseas encolar esta orden para que se ejecute automáticamente cuando el mercado abra?
                </p>
                <p className="text-xs text-text-muted text-center">
                  La orden se ejecutará al precio de mercado vigente en el momento de la apertura.
                </p>
              </div>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={handleClose}
                  className="flex-1 py-2.5 rounded-xl border border-border-default text-text-secondary text-sm font-medium hover:bg-bg-tertiary transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  onClick={handleEnqueue}
                  className="flex-1 py-2.5 rounded-xl bg-gold hover:bg-gold/90 text-bg-base text-sm font-bold transition-colors"
                >
                  Sí, encolar
                </button>
              </div>
            </div>
          )}

          {state === "error" && (
            <div className="flex flex-col items-center gap-4 py-4">
              <div className="w-16 h-16 rounded-full bg-danger/20 flex items-center justify-center">
                <AlertCircle size={32} className="text-danger" />
              </div>
              <div className="text-center">
                <p className="font-display text-xl text-text-primary mb-1">No se pudo procesar la orden</p>
                <p className="text-sm text-text-muted">{errorMsg}</p>
              </div>
              <button
                type="button"
                onClick={() => setState("idle")}
                className="mt-2 px-6 py-2 rounded-xl border border-border-default text-text-secondary text-sm font-medium hover:bg-bg-tertiary transition-colors"
              >
                Intentar de nuevo
              </button>
            </div>
          )}

          {(state === "idle" || state === "loading") && (
            <>
              <h2 className="font-display text-2xl text-text-primary mb-4">{title}</h2>

              <p className="text-sm text-text-secondary mb-1">
                Vas a {isBuy ? "comprar" : "vender"}{" "}
                <span className="font-semibold text-text-primary">
                  {draft.quantity} accion{draft.quantity !== 1 ? "es" : ""}
                </span>{" "}
                de {stockName}{" "}
                <span className="font-semibold text-text-primary">({draft.symbol})</span>.
              </p>
              <p className="text-sm text-text-secondary mb-4">
                Precio estimado:{" "}
                <span className="font-semibold text-text-primary tabular-nums">
                  ${computed.price.toFixed(2)}
                </span>{" "}
                por acción.
              </p>

              <OrderSummary computed={computed} side={draft.side} />

              <div className="mt-4 mb-5 flex items-start gap-2 bg-yellow-500/10 border border-yellow-500/30 rounded-xl px-3 py-2.5">
                <AlertCircle size={14} className="text-yellow-400 mt-0.5 shrink-0" />
                <p className="text-xs text-yellow-300">
                  El precio de mercado puede variar al momento de ejecución.
                  Esta es una estimación basada en el último precio disponible.
                </p>
              </div>

              <div className="flex gap-3">
                <button
                  type="button"
                  disabled={state === "loading"}
                  onClick={handleClose}
                  className="flex-1 py-2.5 rounded-xl border border-border-default text-text-secondary text-sm font-medium hover:bg-bg-tertiary transition-colors duration-150 disabled:opacity-50"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  disabled={state === "loading"}
                  onClick={handleConfirm}
                  className={cn(
                    "flex-1 py-2.5 rounded-xl text-white text-sm font-bold transition-colors duration-150 disabled:opacity-70",
                    actionColor
                  )}
                >
                  {state === "loading" ? (
                    <span className="flex items-center justify-center gap-2">
                      <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                      </svg>
                      Procesando...
                    </span>
                  ) : (
                    "Confirmar"
                  )}
                </button>
              </div>
            </>
          )}

          {state === "queued-loading" && (
            <div className="flex flex-col items-center gap-4 py-8">
              <svg className="animate-spin h-10 w-10 text-gold" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
              </svg>
              <p className="text-sm text-text-secondary">Encolando orden...</p>
            </div>
          )}
        </div>
      </div>

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 bg-bg-elevated border border-success/40 text-success text-sm font-medium px-6 py-3 rounded-full shadow-lg flex items-center gap-2">
          <Check size={16} />
          Orden enviada exitosamente
        </div>
      )}
    </>,
    document.body
  );
}
