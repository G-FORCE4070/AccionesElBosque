export type ReportType =
  | "ganancias_perdidas"
  | "resumen_portafolio"
  | "comisiones"
  | "transacciones"
  | "ordenes"
  | "historico_simbolo";

export interface GpRow {
  periodo: string;
  gananciaBruta: number;
  comisiones: number;
  gananciaNeta: number;
}

export const mockGpReport: GpRow[] = [
  { periodo: "Ene 2025", gananciaBruta:  1250.00, comisiones: -87.50,  gananciaNeta:  1162.50 },
  { periodo: "Feb 2025", gananciaBruta:   890.00, comisiones: -62.30,  gananciaNeta:   827.70 },
  { periodo: "Mar 2025", gananciaBruta:  -340.00, comisiones: -45.20,  gananciaNeta:  -385.20 },
  { periodo: "Abr 2025", gananciaBruta:  2100.00, comisiones: -147.00, gananciaNeta:  1953.00 },
];

export const REPORT_TYPE_OPTIONS = [
  { value: "ganancias_perdidas",  label: "Ganancias y Pérdidas" },
  { value: "resumen_portafolio",  label: "Resumen del portafolio" },
  { value: "comisiones",          label: "Comisiones pagadas" },
  { value: "transacciones",       label: "Transacciones del período" },
  { value: "ordenes",             label: "Órdenes ejecutadas" },
  { value: "historico_simbolo",   label: "Histórico por símbolo" },
];
