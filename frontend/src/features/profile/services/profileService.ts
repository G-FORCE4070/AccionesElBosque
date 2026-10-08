const BASE_URL = "http://localhost:8081/api/v1/users";

function authHeaders(token: string) {
  return { "Content-Type": "application/json", Authorization: `Bearer ${token}` };
}

export interface ProfilePreferences {
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
  market: string | null;
  sector: string | null;
}

export interface ProfileData {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  birthDate: string;
  nationality: string;
  docType: string;
  docNumber: string;
  country: string;
  city: string;
  address: string;
  postalCode: string;
  incomeSource: string;
  annualIncome: string;
  netWorth: string;
  riskTolerance: string;
  tradingExperience: string;
  investmentGoal: string;
  role: string;
  plan: string;
  preferences: ProfilePreferences;
  broker: null;
}

export interface UpdatePersonalPayload {
  // Contact info (editable)
  address?: string;
  city?: string;
  country?: string;
  postalCode?: string;
  // Financial profile (editable)
  incomeSource?: string;
  annualIncome?: string;
  netWorth?: string;
  riskTolerance?: string;
  tradingExperience?: string;
  investmentGoal?: string;
}

export interface UpdatePreferencesPayload {
  marketOpen?: boolean;
  priceAlerts?: boolean;
  dailySummary?: boolean;
  orderConfirm?: boolean;
  notifEmail?: boolean;
  notifSms?: boolean;
  notifWhatsapp?: boolean;
  monthlyReport?: boolean;
  viewType?: string;
  defaultOrder?: string;
  showLowWeight?: boolean;
  defaultOrderType?: string;
  dailyLimit?: number;
  requireDoubleAuth?: boolean;
  market?: string;
  sector?: string;
}

export const profileService = {
  async getProfile(token: string): Promise<ProfileData> {
    const res = await fetch(`${BASE_URL}/me`, { headers: authHeaders(token) });
    const body = await res.json();
    if (!res.ok) throw new Error(body.error ?? "Error al cargar el perfil");
    return body;
  },

  async updatePersonal(token: string, payload: UpdatePersonalPayload): Promise<void> {
    const res = await fetch(`${BASE_URL}/me/personal`, {
      method: "PUT",
      headers: authHeaders(token),
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.error ?? "Error al actualizar datos personales");
    }
  },

  async updatePreferences(token: string, payload: UpdatePreferencesPayload): Promise<void> {
    const res = await fetch(`${BASE_URL}/me/preferences`, {
      method: "PUT",
      headers: authHeaders(token),
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.error ?? "Error al actualizar preferencias");
    }
  },
};
