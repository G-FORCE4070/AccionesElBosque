import { useState, useEffect } from "react";
import { Card } from "../../../components/ui/Card";
import { Badge } from "../../../components/ui/Badge";
import { Button } from "../../../components/ui/Button";
import { useSessionStore } from "../../../store/sessionStore";
import { ALL_SYMBOLS } from "../data/mockProfile";
import { getWatchlist, addSymbol, removeSymbol } from "../../dashboard/services/watchlistService";
import { cn } from "../../../lib/cn";

const SKELETON_CHIPS = ["ck-1", "ck-2", "ck-3", "ck-4", "ck-5", "ck-6"];

export function WatchlistManagerCard() {
  const token = useSessionStore((s) => s.token ?? "");
  const plan = useSessionStore((s) => s.user?.plan ?? "basic");
  const isPremium = plan.toUpperCase() === "PREMIUM";

  const [saved, setSaved] = useState<string[]>([]);
  const [pending, setPending] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveMsg, setSaveMsg] = useState<{ text: string; ok: boolean } | null>(null);

  useEffect(() => {
    if (!isPremium || !token) return;
    setLoading(true);
    getWatchlist(token)
      .then((syms) => {
        setSaved(syms);
        setPending(syms);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [isPremium, token]);

  function toggle(sym: string) {
    setPending((prev) =>
      prev.includes(sym) ? prev.filter((s) => s !== sym) : [...prev, sym]
    );
  }

  const comparator = (a: string, b: string) => a.localeCompare(b);
  const isDirty =
    JSON.stringify([...pending].sort(comparator)) !==
    JSON.stringify([...saved].sort(comparator));

  async function handleSave() {
    setSaving(true);
    setSaveMsg(null);
    try {
      const toAdd = pending.filter((s) => !saved.includes(s));
      const toRemove = saved.filter((s) => !pending.includes(s));
      await Promise.all([
        ...toAdd.map((s) => addSymbol(s, token)),
        ...toRemove.map((s) => removeSymbol(s, token)),
      ]);
      setSaved(pending);
      setSaveMsg({ text: "Guardado", ok: true });
    } catch {
      setSaveMsg({ text: "Error al guardar", ok: false });
    } finally {
      setSaving(false);
      setTimeout(() => setSaveMsg(null), 2500);
    }
  }

  return (
    <Card>
      <div className="flex items-center gap-2 mb-4">
        <h2 className="text-base font-semibold text-gold">Gestión de Watchlist</h2>
        <Badge variant="premium">Premium</Badge>
      </div>

      {isPremium ? (
        <>
          <p className="text-sm text-text-muted mb-4">
            Selecciona las acciones que quieres seguir y guarda los cambios.
          </p>

          {loading ? (
            <div className="flex flex-wrap gap-2">
              {SKELETON_CHIPS.map((key) => (
                <div key={key} className="h-8 w-16 rounded-full bg-bg-tertiary/40 animate-pulse" />
              ))}
            </div>
          ) : (
            <div className="flex flex-wrap gap-2">
              {ALL_SYMBOLS.map((sym) => {
                const active = pending.includes(sym);
                return (
                  <button
                    key={sym}
                    type="button"
                    onClick={() => toggle(sym)}
                    className={cn(
                      "rounded-full px-3 py-1.5 text-xs font-medium transition-colors duration-150",
                      active
                        ? "bg-bg-tertiary border border-gold/30 text-gold"
                        : "bg-bg-tertiary border border-border-default text-text-muted hover:border-border-focus"
                    )}
                  >
                    {sym}
                  </button>
                );
              })}
            </div>
          )}

          <div className="flex items-center gap-3 mt-4">
            <Button
              size="sm"
              variant="primary"
              onClick={handleSave}
              loading={saving}
              disabled={!isDirty || saving}
            >
              Guardar
            </Button>
            {saveMsg && (
              <span className={cn("text-xs", saveMsg.ok ? "text-success" : "text-danger")}>
                {saveMsg.text}
              </span>
            )}
          </div>
        </>
      ) : (
        <div className="flex flex-col gap-3">
          <p className="text-sm text-text-muted">
            Función exclusiva para usuarios Premium.
          </p>
          <Button size="sm" variant="secondary">Actualizar a Premium</Button>
        </div>
      )}
    </Card>
  );
}
