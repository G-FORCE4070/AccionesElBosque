import { useState } from "react";
import { Check } from "lucide-react";
import { PageHeader } from "../../../components/layout/PageHeader";
import { ReportGeneratorForm } from "../components/ReportGeneratorForm";
import { ReportPreview } from "../components/ReportPreview";
import { reportService } from "../services/reportService";
import type { GpRow, ReportType } from "../data/mockReportData";

export function ReportsPage() {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<GpRow[] | null>(null);
  const [activeType, setActiveType] = useState<ReportType>("ganancias_perdidas");
  const [activeFrom, setActiveFrom] = useState("2025-01-01");
  const [activeTo, setActiveTo] = useState("2025-12-31");
  const [toast, setToast] = useState<string | null>(null);

  async function handleGenerate(type: ReportType, from: string, to: string) {
    setLoading(true);
    setActiveType(type);
    setActiveFrom(from);
    setActiveTo(to);
    const data = await reportService.generate({ type, from, to });
    setRows(data);
    setLoading(false);
  }

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 4000);
  }

  return (
    <>
      <PageHeader
        title="Reportes"
        subtitle="Genera informes personalizados y descarga documentos oficiales"
      />
      <ReportGeneratorForm loading={loading} onGenerate={handleGenerate} />
      <ReportPreview
        rows={rows}
        type={activeType}
        from={activeFrom}
        to={activeTo}
        onPdfToast={() => showToast("La generación de PDF estará disponible próximamente.")}
      />

      {toast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 bg-bg-elevated border border-gold/40 text-gold text-sm font-medium px-6 py-3 rounded-full shadow-lg flex items-center gap-2">
          <Check size={16} />
          {toast}
        </div>
      )}
    </>
  );
}
