import { useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";
import { X, Loader2 } from "lucide-react";
import { Card } from "../../../components/ui/Card";
import { Button } from "../../../components/ui/Button";
import { useProfileStore } from "../store/profileStore";
import { useSessionStore } from "../../../store/sessionStore";
import { subscriptionService } from "../services/subscriptionService";

interface CancelSubModalProps {
  onClose: () => void;
  onConfirm: () => void;
  loading: boolean;
  error: string | null;
}

function CancelSubModal({ onClose, onConfirm, loading, error }: CancelSubModalProps) {
  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
      role="presentation"
      onClick={onClose}
      onKeyDown={(e) => { if (e.key === 'Escape') onClose(); }}
    >
      <div
        className="relative bg-bg-elevated border border-border-subtle rounded-2xl p-6 w-full max-w-[440px] shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <button type="button" onClick={onClose} className="absolute top-4 right-4 text-text-subtle hover:text-text-secondary">
          <X size={18} />
        </button>
        <h2 className="font-display text-2xl text-text-primary mb-3">
          ¿Cancelar tu suscripción Premium?
        </h2>
        <p className="text-sm text-text-muted mb-6">
          Perderás acceso a Watchlist personalizada, alertas de precio y demás funciones premium
          de forma inmediata.
        </p>
        {error && <p className="text-xs text-danger mb-4">{error}</p>}
        <div className="flex gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="flex-1 py-2.5 rounded-xl border border-border-default text-text-secondary text-sm font-medium hover:bg-bg-tertiary transition-colors duration-150 disabled:opacity-50"
          >
            Volver
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className="flex-1 py-2.5 rounded-xl border border-danger text-danger text-sm font-bold hover:bg-danger/10 transition-colors duration-150 disabled:opacity-50 flex items-center justify-center gap-2"
          >
            {loading && <Loader2 size={14} className="animate-spin" />}
            Sí, cancelar
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}

interface UpgradeModalProps {
  onClose: () => void;
  onSelect: (cycle: "monthly" | "annual") => void;
  loading: boolean;
  error: string | null;
}

function UpgradeModal({ onClose, onSelect, loading, error }: UpgradeModalProps) {
  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
      onClick={onClose}
    >
      <div
        className="relative bg-bg-elevated border border-border-subtle rounded-2xl p-6 w-full max-w-[440px] shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <button type="button" onClick={onClose} disabled={loading} className="absolute top-4 right-4 text-text-subtle hover:text-text-secondary disabled:opacity-40">
          <X size={18} />
        </button>
        <h2 className="font-display text-2xl text-text-primary mb-1">Suscripción Premium</h2>
        <p className="text-sm text-text-muted mb-6">
          Accede a Watchlist personalizada y alertas de precio. Elige tu plan:
        </p>

        <div className="flex flex-col gap-3">
          <button
            type="button"
            disabled={loading}
            onClick={() => onSelect("monthly")}
            className="w-full text-left p-4 rounded-xl border border-border-default hover:border-gold/60 hover:bg-gold/5 transition-colors duration-150 disabled:opacity-50"
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold text-text-primary">Mensual</p>
                <p className="text-xs text-text-muted mt-0.5">Facturado cada mes</p>
              </div>
              <p className="text-lg font-bold text-gold">$12<span className="text-xs font-normal text-text-muted">/mes</span></p>
            </div>
          </button>

          <button
            type="button"
            disabled={loading}
            onClick={() => onSelect("annual")}
            className="w-full text-left p-4 rounded-xl border border-gold/40 hover:border-gold hover:bg-gold/5 transition-colors duration-150 disabled:opacity-50 relative"
          >
            <span className="absolute top-2 right-2 text-[10px] bg-gold/20 text-gold px-2 py-0.5 rounded-full font-semibold">
              Ahorra $24
            </span>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold text-text-primary">Anual</p>
                <p className="text-xs text-text-muted mt-0.5">Facturado una vez al año</p>
              </div>
              <p className="text-lg font-bold text-gold">$120<span className="text-xs font-normal text-text-muted">/año</span></p>
            </div>
          </button>
        </div>

        {loading && (
          <div className="flex items-center justify-center gap-2 mt-4 text-sm text-text-muted">
            <Loader2 size={16} className="animate-spin" />
            Redirigiendo a Stripe…
          </div>
        )}
        {error && <p className="text-xs text-danger mt-4">{error}</p>}
      </div>
    </div>,
    document.body
  );
}

export function SubscriptionCard() {
  const { subscription, updateSubscription } = useProfileStore();
  const token = useSessionStore((s) => s.token);
  const clearSession = useSessionStore((s) => s.clearSession);
  const navigate = useNavigate();
  const isPremium = subscription.plan === "premium";

  const [upgradeOpen, setUpgradeOpen] = useState(false);
  const [upgradeLoading, setUpgradeLoading] = useState(false);
  const [upgradeError, setUpgradeError] = useState<string | null>(null);

  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelLoading, setCancelLoading] = useState(false);
  const [cancelError, setCancelError] = useState<string | null>(null);

  async function handleUpgrade(cycle: "monthly" | "annual") {
    if (!token) return;
    setUpgradeLoading(true);
    setUpgradeError(null);
    try {
      const url = await subscriptionService.checkout(cycle, token);
      window.location.href = url;
    } catch (e: unknown) {
      setUpgradeError(e instanceof Error ? e.message : "Error al iniciar el proceso.");
      setUpgradeLoading(false);
    }
  }

  async function handleCancelConfirm() {
    if (!token) return;
    setCancelLoading(true);
    setCancelError(null);
    try {
      await subscriptionService.cancelSubscription(token);
      updateSubscription("basic");
      clearSession();
      navigate("/login", {
        state: { successMessage: "Tu suscripción fue cancelada. Inicia sesión para continuar." },
      });
    } catch (e: unknown) {
      setCancelError(e instanceof Error ? e.message : "Error al cancelar.");
    } finally {
      setCancelLoading(false);
    }
  }

  return (
    <>
      <Card>
        <h2 className="text-base font-semibold text-gold mb-4">Suscripción</h2>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-text-secondary">
              Plan actual:{" "}
              <span className="text-base font-bold text-gold capitalize">
                {isPremium ? "Premium" : "Básico"}
              </span>
            </p>
            {isPremium && subscription.features.length > 0 && (
              <p className="text-xs text-text-muted mt-1">{subscription.features.join(", ")}</p>
            )}
            {!isPremium && (
              <p className="text-xs text-text-muted mt-1">
                Actualiza a Premium para acceder a Watchlist personalizada y alertas de precio.
              </p>
            )}
          </div>
          <div className="flex items-center gap-2">
            {isPremium ? (
              <>
                <Button variant="danger" size="sm" onClick={() => { setCancelError(null); setCancelOpen(true); }}>
                  Cancelar suscripción
                </Button>
              </>
            ) : (
              <Button size="sm" onClick={() => { setUpgradeError(null); setUpgradeOpen(true); }}>
                Actualizar a Premium
              </Button>
            )}
          </div>
        </div>
      </Card>

      {upgradeOpen && (
        <UpgradeModal
          onClose={() => { if (!upgradeLoading) setUpgradeOpen(false); }}
          onSelect={handleUpgrade}
          loading={upgradeLoading}
          error={upgradeError}
        />
      )}

      {cancelOpen && (
        <CancelSubModal
          onClose={() => { if (!cancelLoading) setCancelOpen(false); }}
          onConfirm={handleCancelConfirm}
          loading={cancelLoading}
          error={cancelError}
        />
      )}
    </>
  );
}
