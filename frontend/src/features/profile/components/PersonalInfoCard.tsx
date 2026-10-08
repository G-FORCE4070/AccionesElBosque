import { useState } from "react";
import { Card } from "../../../components/ui/Card";
import { Button } from "../../../components/ui/Button";
import { EditPersonalInfoModal } from "./EditPersonalInfoModal";
import { useProfileStore } from "../store/profileStore";
import {
  COUNTRIES, NATIONALITIES, INCOME_SOURCES,
  ANNUAL_INCOME_LABELS, NET_WORTH_LABELS, RISK_LABELS,
  EXPERIENCE_LABELS, INVESTMENT_GOAL_LABELS,
} from "../data/profileConstants";

const DOC_TYPE_LABELS: Record<string, string> = {
  cc: "Cédula de Ciudadanía",
  ce: "Cédula de Extranjería",
  passport: "Pasaporte",
};

function InfoField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-wide text-text-muted mb-2">{label}</p>
      <p className="text-sm text-text-secondary border-b border-border-subtle/30 pb-3">
        {value || "—"}
      </p>
    </div>
  );
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-xs uppercase tracking-wide text-gold-light/70 font-semibold mt-6 mb-3">
      {children}
    </p>
  );
}

export function PersonalInfoCard() {
  const p = useProfileStore((s) => s.personal);
  const [editOpen, setEditOpen] = useState(false);

  return (
    <>
      <Card>
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-gold">Información Personal</h2>
          <Button size="sm" onClick={() => setEditOpen(true)}>
            Actualizar datos
          </Button>
        </div>
        <div className="border-t border-border-subtle/40 my-4" />

        <SectionTitle>Identidad</SectionTitle>
        <div className="grid grid-cols-2 gap-x-12 gap-y-6">
          <InfoField label="Nombre" value={p.firstName} />
          <InfoField label="Apellido" value={p.lastName} />
          <InfoField label="Correo electrónico" value={p.email} />
          <InfoField label="Celular" value={p.phone} />
          <InfoField label="Fecha de nacimiento" value={p.birthDate} />
          <InfoField label="Nacionalidad" value={NATIONALITIES[p.nationality] ?? p.nationality} />
          <InfoField label="Tipo de documento" value={DOC_TYPE_LABELS[p.docType] ?? p.docType} />
          <InfoField label="Número de documento" value={p.docNumber} />
        </div>

        <SectionTitle>Residencia</SectionTitle>
        <div className="grid grid-cols-2 gap-x-12 gap-y-6">
          <InfoField label="País de residencia" value={COUNTRIES[p.country] ?? p.country} />
          <InfoField label="Ciudad" value={p.city} />
          <InfoField label="Dirección" value={p.address} />
          <InfoField label="Código postal" value={p.postalCode} />
        </div>

        <SectionTitle>Perfil de inversión</SectionTitle>
        <div className="grid grid-cols-2 gap-x-12 gap-y-6">
          <InfoField label="Fuente de ingresos" value={INCOME_SOURCES[p.incomeSource] ?? p.incomeSource} />
          <InfoField label="Ingreso anual" value={ANNUAL_INCOME_LABELS[p.annualIncome] ?? p.annualIncome} />
          <InfoField label="Patrimonio total" value={NET_WORTH_LABELS[p.netWorth] ?? p.netWorth} />
          <InfoField label="Tolerancia al riesgo" value={RISK_LABELS[p.riskTolerance] ?? p.riskTolerance} />
          <InfoField label="Experiencia en trading" value={EXPERIENCE_LABELS[p.tradingExperience] ?? p.tradingExperience} />
          <InfoField label="Objetivo de inversión" value={INVESTMENT_GOAL_LABELS[p.investmentGoal] ?? p.investmentGoal} />
        </div>
      </Card>
      <EditPersonalInfoModal open={editOpen} onClose={() => setEditOpen(false)} />
    </>
  );
}
