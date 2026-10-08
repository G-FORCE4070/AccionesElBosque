import { FileBarChart, FileText, FileSpreadsheet } from "lucide-react";
import { ReportTable } from "./ReportTable";
import type { GpRow, ReportType } from "../data/mockReportData";
import { reportService } from "../services/reportService";
import { REPORT_TYPE_OPTIONS } from "../data/mockReportData";

interface ReportPreviewProps {
  rows: GpRow[] | null;
  type: ReportType;
  from: string;
  to: string;
  onPdfToast: () => void;
}

export function ReportPreview({ rows, type, from, to, onPdfToast }: ReportPreviewProps) {
  const typeLabel = REPORT_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? type;

  return (
    <div className="bg-bg-secondary rounded-2xl p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <span className="text-sm font-medium text-text-secondary">
          Vista previa: {typeLabel} ({from} — {to})
        </span>
        {rows && (
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => rows && reportService.downloadPdf(rows, from, to)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-border-default text-sm text-text-secondary hover:border-border-focus hover:text-text-primary transition-colors duration-150"
            >
              <FileText size={14} />
              Descargar PDF
            </button>
            <button
              type="button"
              onClick={() => rows && reportService.downloadCsv(rows, from, to)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl border border-border-default text-sm text-text-secondary hover:border-border-focus hover:text-text-primary transition-colors duration-150"
            >
              <FileSpreadsheet size={14} />
              Descargar CSV
            </button>
          </div>
        )}
      </div>

      {/* Content */}
      {rows === null ? (
        <div className="flex flex-col items-center justify-center py-16 gap-4">
          <FileBarChart size={48} className="text-text-muted" />
          <p className="text-sm text-text-muted text-center">
            Selecciona un tipo de reporte y un período para generar la vista previa.
          </p>
        </div>
      ) : type === "ganancias_perdidas" ? (
        <ReportTable rows={rows} />
      ) : (
        <div className="flex flex-col items-center justify-center py-14 gap-3">
          <FileBarChart size={40} className="text-text-muted" />
          <p className="text-base font-semibold text-text-secondary">Reporte en construcción</p>
          <p className="text-sm text-text-muted">Este tipo de reporte estará disponible próximamente.</p>
        </div>
      )}
    </div>
  );
}
