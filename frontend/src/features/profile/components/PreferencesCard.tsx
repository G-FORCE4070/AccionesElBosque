import { useState, useEffect, useRef } from "react";
import { Card } from "../../../components/ui/Card";
import { Badge } from "../../../components/ui/Badge";
import { Button } from "../../../components/ui/Button";
import { ToggleRow } from "./ToggleRow";
import { SelectRow } from "./SelectRow";
import { NumberInputRow } from "./NumberInputRow";
import { useProfileStore } from "../store/profileStore";
import { useSessionStore } from "../../../store/sessionStore";

type Draft = {
  marketOpen: boolean;
  priceAlerts: boolean;
  dailySummary: boolean;
  orderConfirm: boolean;
  notifEmail: boolean;
  notifSms: boolean;
  notifWhatsapp: boolean;
  monthlyReport: boolean;
  viewType: string;
  defaultOrder: string;
  showLowWeight: boolean;
  defaultOrderType: string;
  dailyLimit: number;
  requireDoubleAuth: boolean;
  market: string;
  sector: string;
};

function buildDraft(s: ReturnType<typeof useProfileStore.getState>): Draft {
  return {
    ...s.notifications,
    ...s.notifChannels,
    ...s.portfolio,
    ...s.operations,
    market: s.market,
    sector: s.sector,
  };
}

export function PreferencesCard() {
  const { updatePreferences, loaded } = useProfileStore();
  const subscription = useProfileStore((s) => s.subscription);
  const token = useSessionStore((s) => s.token);
  const isPremium = subscription.plan === "premium";

  const [draft, setDraft] = useState<Draft>(() => buildDraft(useProfileStore.getState()));
  const loadedRef = useRef(false);

  // Sync draft from store once when loadProfile completes
  useEffect(() => {
    if (loaded && !loadedRef.current) {
      loadedRef.current = true;
      setDraft(buildDraft(useProfileStore.getState()));
    }
  }, [loaded]);

  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function set<K extends keyof Draft>(k: K, v: Draft[K]) {
    setDraft((d) => ({ ...d, [k]: v }));
  }

  async function handleSave() {
    if (!token) return;
    setSaving(true);
    setError(null);
    try {
      await updatePreferences(token, draft);
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card>
      <h2 className="text-base font-semibold text-gold mb-4">Preferencias</h2>

      {/* Notification toggles */}
      <p className="text-sm font-semibold text-gold-light mb-1">Notificaciones</p>
      <ToggleRow
        label="Apertura y cierre de mercados"
        checked={draft.marketOpen}
        onChange={(v) => set("marketOpen", v)}
        disabled
      />
      <ToggleRow
        label={
          <span className="flex items-center gap-2">
            Alertas de precios
            <Badge variant="premium" className="text-[10px] py-0.5 px-2">Premium</Badge>
          </span>
        }
        checked={draft.priceAlerts}
        onChange={(v) => set("priceAlerts", v)}
        disabled={!isPremium}
      />
      <ToggleRow
        label="Resumen diario de operaciones"
        checked={draft.dailySummary}
        onChange={(v) => set("dailySummary", v)}
        disabled
      />
      <ToggleRow
        label="Confirmación de órdenes ejecutadas"
        checked={draft.orderConfirm}
        onChange={(v) => set("orderConfirm", v)}
      />

      {/* Notification channels */}
      <p className="text-sm font-semibold text-gold-light mt-6 mb-1">Canal de notificaciones</p>
      <ToggleRow
        label="Correo electrónico"
        checked={draft.notifEmail}
        onChange={(v) => set("notifEmail", v)}
      />
      <ToggleRow
        label={
          <span className="flex items-center gap-2">
            SMS
            <span className="text-[10px] text-text-muted border border-border-default rounded-full px-2 py-0.5">Próximamente</span>
          </span>
        }
        checked={draft.notifSms}
        onChange={() => {}}
        disabled
      />
      <ToggleRow
        label={
          <span className="flex items-center gap-2">
            WhatsApp
            <span className="text-[10px] text-text-muted border border-border-default rounded-full px-2 py-0.5">Próximamente</span>
          </span>
        }
        checked={draft.notifWhatsapp}
        onChange={() => {}}
        disabled
      />

      {/* Market and sector */}
      <p className="text-sm font-semibold text-gold-light mt-6 mb-1">Mercado y Sector</p>
      <SelectRow
        label="Mercado preferido"
        value={draft.market}
        onChange={(v) => set("market", v)}
        options={[
          { value: "nyse",   label: "NYSE" },
          { value: "nasdaq", label: "NASDAQ" },
        ]}
      />
      <SelectRow
        label="Sector de interés"
        value={draft.sector}
        onChange={(v) => set("sector", v)}
        options={[
          { value: "tech",     label: "Tecnología" },
          { value: "health",   label: "Salud" },
          { value: "consumer", label: "Consumo" },
        ]}
      />

      {/* Portfolio visualization */}
      <p className="text-sm font-semibold text-gold-light mt-6 mb-1">Visualización del Portafolio</p>
      <SelectRow
        label="Tipo de visualización"
        value={draft.viewType}
        onChange={(v) => set("viewType", v)}
        options={[
          { value: "list",   label: "Lista" },
          { value: "bars",   label: "Gráfico de barras" },
          { value: "detail", label: "Vista detallada" },
        ]}
      />
      <SelectRow
        label="Orden predeterminado"
        value={draft.defaultOrder}
        onChange={(v) => set("defaultOrder", v)}
        options={[
          { value: "totalValue", label: "Valor total" },
          { value: "gainPct",    label: "% G/P" },
          { value: "symbol",     label: "Símbolo" },
        ]}
      />
      <ToggleRow
        label="Mostrar activos con bajo peso (<1%)"
        checked={draft.showLowWeight}
        onChange={(v) => set("showLowWeight", v)}
      />

      {/* Operations */}
      <p className="text-sm font-semibold text-gold-light mt-6 mb-1">Configuración de Operaciones</p>
      <SelectRow
        label="Tipo de orden predeterminado"
        value={draft.defaultOrderType}
        onChange={(v) => set("defaultOrderType", v)}
        options={[
          { value: "market", label: "Market" },
        ]}
      />
      <NumberInputRow
        label="Límite de operaciones diarias"
        value={draft.dailyLimit}
        onChange={(v) => set("dailyLimit", v)}
        min={1}
        max={100}
      />
      <ToggleRow
        label="Autorización doble para órdenes > $10,000"
        checked={draft.requireDoubleAuth}
        onChange={(v) => set("requireDoubleAuth", v)}
      />

      {/* Save */}
      <div className="flex items-center justify-end gap-3 mt-6 pt-4 border-t border-border-subtle/30">
        {error && <p className="text-xs text-danger mr-auto">{error}</p>}
        {saved && <p className="text-xs text-success mr-auto">Preferencias guardadas.</p>}
        <Button size="sm" loading={saving} onClick={handleSave}>
          Guardar preferencias
        </Button>
      </div>
    </Card>
  );
}
