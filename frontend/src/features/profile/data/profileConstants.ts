export const COUNTRIES: Record<string, string> = {
  CO: "Colombia", US: "Estados Unidos", MX: "México", AR: "Argentina",
  BR: "Brasil", CL: "Chile", PE: "Perú", VE: "Venezuela",
  EC: "Ecuador", BO: "Bolivia", PY: "Paraguay", UY: "Uruguay",
  PA: "Panamá", CR: "Costa Rica", GT: "Guatemala", DO: "República Dominicana",
  ES: "España", GB: "Reino Unido", DE: "Alemania", FR: "Francia",
  CA: "Canadá", AU: "Australia",
};

export const NATIONALITIES: Record<string, string> = {
  CO: "Colombiana/o", US: "Estadounidense", MX: "Mexicana/o", AR: "Argentina/o",
  BR: "Brasileña/o", CL: "Chilena/o", PE: "Peruana/o", VE: "Venezolana/o",
  EC: "Ecuatoriana/o", BO: "Boliviana/o", PY: "Paraguaya/o", UY: "Uruguaya/o",
  PA: "Panameña/o", CR: "Costarricense", GT: "Guatemalteca/o", DO: "Dominicana/o",
  ES: "Española/ol", GB: "Británica/o", DE: "Alemana/o", FR: "Francesa/és",
  CA: "Canadiense", AU: "Australiana/o",
};

export const INCOME_SOURCES: Record<string, string> = {
  salary: "Salario", business: "Negocio propio", investments: "Inversiones",
  pension: "Pensión", other: "Otro",
};

export const ANNUAL_INCOME_LABELS: Record<string, string> = {
  "<30k": "Menos de $30k", "30k-100k": "$30k – $100k",
  "100k-500k": "$100k – $500k", ">500k": "Más de $500k",
};

export const NET_WORTH_LABELS: Record<string, string> = {
  "<50k": "Menos de $50k", "50k-250k": "$50k – $250k", ">250k": "Más de $250k",
};

export const RISK_LABELS: Record<string, string> = {
  conservative: "Conservador", moderate: "Moderado", aggressive: "Agresivo",
};

export const EXPERIENCE_LABELS: Record<string, string> = {
  beginner: "Principiante", experienced: "Con experiencia",
};

export const INVESTMENT_GOAL_LABELS: Record<string, string> = {
  preservation: "Preservación de capital", income: "Ingreso periódico",
  growth: "Crecimiento a largo plazo", speculation: "Especulación a corto plazo",
};

export const COUNTRY_OPTIONS = Object.entries(COUNTRIES).map(([value, label]) => ({ value, label }));

export const INCOME_SOURCE_OPTIONS = Object.entries(INCOME_SOURCES).map(([value, label]) => ({ value, label }));

export const ANNUAL_INCOME_OPTIONS = Object.entries(ANNUAL_INCOME_LABELS).map(([value, label]) => ({ value, label }));

export const NET_WORTH_OPTIONS = Object.entries(NET_WORTH_LABELS).map(([value, label]) => ({ value, label }));

export const RISK_OPTIONS = Object.entries(RISK_LABELS).map(([value, label]) => ({ value, label }));

export const EXPERIENCE_OPTIONS = Object.entries(EXPERIENCE_LABELS).map(([value, label]) => ({ value, label }));

export const INVESTMENT_GOAL_OPTIONS = Object.entries(INVESTMENT_GOAL_LABELS).map(([value, label]) => ({ value, label }));
