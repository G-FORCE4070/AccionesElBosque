import { useState } from "react";
import { Button } from "../../../components/ui/Button";
import { Select } from "../../../components/ui/Select";
import { REPORT_TYPE_OPTIONS, type ReportType } from "../data/mockReportData";

interface ReportGeneratorFormProps {
  loading: boolean;
  onGenerate: (type: ReportType, from: string, to: string) => void;
}

const inputClass =
  "bg-bg-tertiary border border-border-default rounded-xl h-11 px-4 text-sm text-text-secondary focus:outline-none focus:border-border-focus transition-colors duration-150";

export function ReportGeneratorForm({ loading, onGenerate }: ReportGeneratorFormProps) {
  const [type, setType] = useState<ReportType>("ganancias_perdidas");
  const [from, setFrom] = useState("2025-01-01");
  const [to, setTo] = useState("2025-12-31");

  return (
    <div className="bg-bg-secondary rounded-2xl p-6 mb-4">
      <p className="text-base font-semibold text-text-secondary mb-4">
        Generar reporte personalizado
      </p>

      <div className="flex items-end gap-4 flex-wrap">
        <div className="min-w-[220px]">
          <Select
            label="Tipo de reporte"
            options={REPORT_TYPE_OPTIONS}
            value={type}
            onChange={(v) => setType(v as ReportType)}
          />
        </div>

        <div className="flex flex-col gap-2">
          <label className="text-xs uppercase tracking-wide text-text-muted font-medium">
            Período
          </label>
          <div className="flex items-center gap-2">
            <input
              type="date"
              className={inputClass}
              value={from}
              onChange={(e) => setFrom(e.target.value)}
            />
            <span className="text-text-subtle">—</span>
            <input
              type="date"
              className={inputClass}
              value={to}
              onChange={(e) => setTo(e.target.value)}
            />
          </div>
        </div>

        <Button
          className="ml-auto"
          loading={loading}
          onClick={() => onGenerate(type, from, to)}
        >
          Generar vista previa
        </Button>
      </div>
    </div>
  );
}
