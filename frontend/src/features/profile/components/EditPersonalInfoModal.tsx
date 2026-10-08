import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import { Input } from "../../../components/ui/Input";
import { Select } from "../../../components/ui/Select";
import { Button } from "../../../components/ui/Button";
import { useProfileStore } from "../store/profileStore";
import { useSessionStore } from "../../../store/sessionStore";
import {
  COUNTRY_OPTIONS, INCOME_SOURCE_OPTIONS, ANNUAL_INCOME_OPTIONS,
  NET_WORTH_OPTIONS, RISK_OPTIONS, EXPERIENCE_OPTIONS, INVESTMENT_GOAL_OPTIONS,
} from "../data/profileConstants";

interface EditPersonalInfoModalProps {
  open: boolean;
  onClose: () => void;
}

type FormState = {
  address: string;
  city: string;
  country: string;
  postalCode: string;
  incomeSource: string;
  annualIncome: string;
  netWorth: string;
  riskTolerance: string;
  tradingExperience: string;
  investmentGoal: string;
};

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-xs uppercase tracking-wide text-gold-light/70 font-semibold mt-5 mb-3">
      {children}
    </p>
  );
}

export function EditPersonalInfoModal({ open, onClose }: EditPersonalInfoModalProps) {
  const { updatePersonal } = useProfileStore();
  const token = useSessionStore((s) => s.token);

  const [form, setForm] = useState<FormState>({
    address: "", city: "", country: "", postalCode: "",
    incomeSource: "", annualIncome: "", netWorth: "",
    riskTolerance: "", tradingExperience: "", investmentGoal: "",
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      const p = useProfileStore.getState().personal;
      setForm({
        address: p.address,
        city: p.city,
        country: p.country,
        postalCode: p.postalCode,
        incomeSource: p.incomeSource,
        annualIncome: p.annualIncome,
        netWorth: p.netWorth,
        riskTolerance: p.riskTolerance,
        tradingExperience: p.tradingExperience,
        investmentGoal: p.investmentGoal,
      });
      setError(null);
    }
  }, [open]);

  if (!open) return null;

  function update<K extends keyof FormState>(k: K, v: FormState[K]) {
    setForm((f) => ({ ...f, [k]: v }));
  }

  async function handleSave() {
    if (!token) return;
    setSaving(true);
    setError(null);
    try {
      await updatePersonal(token, form);
      onClose();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Error al guardar");
    } finally {
      setSaving(false);
    }
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4"
      role="presentation"
      onClick={onClose}
      onKeyDown={(e) => { if (e.key === 'Escape') onClose(); }}
    >
      <div
        className="relative bg-bg-elevated border border-border-subtle rounded-2xl p-6 w-full max-w-[600px] shadow-2xl max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 text-text-subtle hover:text-text-secondary"
        >
          <X size={18} />
        </button>
        <h2 className="font-display text-2xl text-text-primary mb-1">Actualizar datos</h2>
        <p className="text-xs text-text-muted mb-2">
          Los datos de identidad (nombre, fecha de nacimiento, documento, nacionalidad) no son modificables por regulaciones KYC.
        </p>

        <SectionLabel>Residencia</SectionLabel>
        <div className="grid grid-cols-2 gap-4">
          <div className="col-span-2">
            <Input
              label="Dirección"
              value={form.address}
              onChange={(e) => update("address", e.target.value)}
            />
          </div>
          <Input label="Ciudad" value={form.city} onChange={(e) => update("city", e.target.value)} />
          <Input label="Código postal" value={form.postalCode} onChange={(e) => update("postalCode", e.target.value)} />
          <div className="col-span-2">
            <Select
              label="País de residencia"
              options={COUNTRY_OPTIONS}
              value={form.country}
              onChange={(v) => update("country", v)}
              placeholder="Seleccione..."
            />
          </div>
        </div>

        <SectionLabel>Perfil de inversión</SectionLabel>
        <div className="grid grid-cols-2 gap-4">
          <Select
            label="Fuente de ingresos"
            options={INCOME_SOURCE_OPTIONS}
            value={form.incomeSource}
            onChange={(v) => update("incomeSource", v)}
            placeholder="Seleccione..."
          />
          <Select
            label="Ingreso anual (USD)"
            options={ANNUAL_INCOME_OPTIONS}
            value={form.annualIncome}
            onChange={(v) => update("annualIncome", v)}
            placeholder="Seleccione..."
          />
          <Select
            label="Patrimonio total (USD)"
            options={NET_WORTH_OPTIONS}
            value={form.netWorth}
            onChange={(v) => update("netWorth", v)}
            placeholder="Seleccione..."
          />
          <Select
            label="Tolerancia al riesgo"
            options={RISK_OPTIONS}
            value={form.riskTolerance}
            onChange={(v) => update("riskTolerance", v)}
            placeholder="Seleccione..."
          />
          <Select
            label="Experiencia en trading"
            options={EXPERIENCE_OPTIONS}
            value={form.tradingExperience}
            onChange={(v) => update("tradingExperience", v)}
            placeholder="Seleccione..."
          />
          <Select
            label="Objetivo de inversión"
            options={INVESTMENT_GOAL_OPTIONS}
            value={form.investmentGoal}
            onChange={(v) => update("investmentGoal", v)}
            placeholder="Seleccione..."
          />
        </div>

        {error && <p className="text-xs text-danger mt-4">{error}</p>}

        <div className="flex gap-3 mt-6">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 py-2.5 rounded-xl border border-border-default text-text-secondary text-sm font-medium hover:bg-bg-tertiary transition-colors duration-150"
          >
            Cancelar
          </button>
          <Button className="flex-1" loading={saving} onClick={handleSave}>
            Guardar cambios
          </Button>
        </div>
      </div>
    </div>,
    document.body
  );
}
