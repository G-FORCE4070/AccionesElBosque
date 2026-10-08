import jsPDF from "jspdf";
import { mockGpReport, type ReportType, type GpRow } from "../data/mockReportData";

export interface ReportParams {
  type: ReportType;
  from: string;
  to: string;
}

export const reportService = {
  async generate(params: ReportParams): Promise<GpRow[] | null> {
    await new Promise((r) => setTimeout(r, 800));
    if (params.type === "ganancias_perdidas") return mockGpReport;
    return null;
  },

  downloadCsv(rows: GpRow[], from: string, to: string) {
    const header = "Período,Ganancia Bruta,Comisiones,Ganancia Neta";
    const body = rows
      .map((r) =>
        [r.periodo, r.gananciaBruta.toFixed(2), r.comisiones.toFixed(2), r.gananciaNeta.toFixed(2)].join(",")
      )
      .join("\n");

    const total = rows.reduce(
      (acc, r) => ({ gb: acc.gb + r.gananciaBruta, com: acc.com + r.comisiones, gn: acc.gn + r.gananciaNeta }),
      { gb: 0, com: 0, gn: 0 }
    );
    const totalRow = `Total,${total.gb.toFixed(2)},${total.com.toFixed(2)},${total.gn.toFixed(2)}`;
    const csv = [header, body, totalRow].join("\n");

    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement("a");
    a.href     = url;
    a.download = `reporte-gp-${from}-${to}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  },

  downloadPdf(rows: GpRow[], from: string, to: string) {
    const total = rows.reduce(
      (acc, r) => ({ gb: acc.gb + r.gananciaBruta, com: acc.com + r.comisiones, gn: acc.gn + r.gananciaNeta }),
      { gb: 0, com: 0, gn: 0 }
    );

    const doc = new jsPDF({ unit: "mm", format: "a4" });
    const W = doc.internal.pageSize.getWidth();

    // Header
    doc.setFillColor(10, 14, 26);
    doc.rect(0, 0, W, 30, "F");
    doc.setTextColor(212, 165, 116);
    doc.setFontSize(16);
    doc.setFont("helvetica", "bold");
    doc.text("Acciones ElBosque", 14, 13);
    doc.setTextColor(156, 163, 175);
    doc.setFontSize(9);
    doc.setFont("helvetica", "normal");
    doc.text("Plataforma profesional de day trading", 14, 20);

    // Title
    doc.setTextColor(17, 17, 17);
    doc.setFontSize(14);
    doc.setFont("helvetica", "bold");
    doc.text("Reporte — Ganancias y Pérdidas", 14, 42);
    doc.setFontSize(9);
    doc.setFont("helvetica", "normal");
    doc.setTextColor(100, 100, 100);
    doc.text(`Período: ${from}  —  ${to}`, 14, 50);

    // Table header background
    const colX = [14, 70, 120, 162];
    const colW = [52, 46, 38, 34];
    let y = 62;

    doc.setFillColor(240, 240, 240);
    doc.rect(14, y - 5, W - 28, 9, "F");
    doc.setFontSize(8);
    doc.setFont("helvetica", "bold");
    doc.setTextColor(60, 60, 60);
    ["Período", "Ganancia bruta", "Comisiones", "Ganancia neta"].forEach((h, i) => {
      doc.text(h, colX[i] + (i > 0 ? colW[i] : 0) / (i > 0 ? 1.2 : 1), y, {
        align: i === 0 ? "left" : "right",
        maxWidth: colW[i],
      });
    });

    y += 6;
    doc.setFont("helvetica", "normal");
    doc.setFontSize(9);

    rows.forEach((r, idx) => {
      if (idx % 2 === 0) {
        doc.setFillColor(250, 250, 250);
        doc.rect(14, y - 4.5, W - 28, 8, "F");
      }
      doc.setTextColor(40, 40, 40);
      doc.text(r.periodo, colX[0], y);

      const gpColor = (n: number) => n >= 0 ? [5, 150, 105] : [220, 38, 38];
      doc.setTextColor(...gpColor(r.gananciaBruta) as [number, number, number]);
      doc.text(`$${r.gananciaBruta.toFixed(2)}`, colX[1] + colW[1], y, { align: "right" });

      doc.setTextColor(40, 40, 40);
      doc.text(`$${r.comisiones.toFixed(2)}`, colX[2] + colW[2], y, { align: "right" });

      doc.setTextColor(...gpColor(r.gananciaNeta) as [number, number, number]);
      doc.text(`$${r.gananciaNeta.toFixed(2)}`, colX[3] + colW[3], y, { align: "right" });

      y += 8;
    });

    // Total row
    doc.setDrawColor(180, 180, 180);
    doc.line(14, y - 2, W - 14, y - 2);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(9);
    doc.setTextColor(40, 40, 40);
    doc.text("Total", colX[0], y + 4);

    const gpC = (n: number) => n >= 0 ? [5, 150, 105] as [number,number,number] : [220, 38, 38] as [number,number,number];
    doc.setTextColor(...gpC(total.gb));
    doc.text(`$${total.gb.toFixed(2)}`, colX[1] + colW[1], y + 4, { align: "right" });
    doc.setTextColor(40, 40, 40);
    doc.text(`$${total.com.toFixed(2)}`, colX[2] + colW[2], y + 4, { align: "right" });
    doc.setTextColor(...gpC(total.gn));
    doc.text(`$${total.gn.toFixed(2)}`, colX[3] + colW[3], y + 4, { align: "right" });

    // Footer
    doc.setFontSize(7);
    doc.setFont("helvetica", "normal");
    doc.setTextColor(160, 160, 160);
    doc.text(`Generado el ${new Date().toLocaleDateString("es-CO")}  ·  Acciones ElBosque`, W / 2, 285, { align: "center" });

    doc.save(`reporte-gp-${from}-${to}.pdf`);
  },
};
