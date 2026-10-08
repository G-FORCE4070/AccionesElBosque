import { cn } from "../../../lib/cn";
import type { GpRow } from "../data/mockReportData";

const fmt = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function fmtSigned(n: number) {
  return (n >= 0 ? "+" : "") + "$" + fmt.format(Math.abs(n));
}

function fmtCommission(n: number) {
  return "-$" + fmt.format(Math.abs(n));
}

function signColor(n: number) {
  return n >= 0 ? "text-success" : "text-danger";
}

interface ReportTableProps {
  rows: GpRow[];
}

export function ReportTable({ rows }: ReportTableProps) {
  const total = rows.reduce(
    (acc, r) => ({
      gb: acc.gb + r.gananciaBruta,
      com: acc.com + r.comisiones,
      gn: acc.gn + r.gananciaNeta,
    }),
    { gb: 0, com: 0, gn: 0 }
  );

  return (
    <table className="w-full">
      <thead>
        <tr>
          {["PERÍODO", "GANANCIA BRUTA", "COMISIONES", "GANANCIA NETA"].map((col) => (
            <th key={col} className="text-xs uppercase tracking-wide text-text-muted py-3 text-left pr-4 last:pr-0">
              {col}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.periodo} className="border-b border-border-subtle/40">
            <td className="py-3 pr-4 text-sm text-text-secondary">{row.periodo}</td>
            <td className={cn("py-3 pr-4 text-sm tabular-nums", signColor(row.gananciaBruta))}>
              {fmtSigned(row.gananciaBruta)}
            </td>
            <td className="py-3 pr-4 text-sm tabular-nums text-danger">
              {fmtCommission(row.comisiones)}
            </td>
            <td className={cn("py-3 text-sm tabular-nums", signColor(row.gananciaNeta))}>
              {fmtSigned(row.gananciaNeta)}
            </td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr className="border-t border-border-subtle/60">
          <td className="pt-3 pb-1 text-base font-bold text-text-primary">Total</td>
          <td className={cn("pt-3 pb-1 text-base font-bold tabular-nums", signColor(total.gb))}>
            {fmtSigned(total.gb)}
          </td>
          <td className="pt-3 pb-1 text-base font-bold tabular-nums text-danger">
            {fmtCommission(total.com)}
          </td>
          <td className={cn("pt-3 pb-1 text-base font-bold tabular-nums", signColor(total.gn))}>
            {fmtSigned(total.gn)}
          </td>
        </tr>
      </tfoot>
    </table>
  );
}
