import type { StoreData } from "../store/registerStore";

const BASE_URL = "http://localhost:8081/api/v1";

export interface RegisterResponse {
  message: string;
  checkoutUrl: string | null;
}

function buildPayload(data: StoreData) {
  const { access, identity, financial, compliance, preferences, plan } = data;
  return {
    firstName: access!.firstName,
    lastName: access!.lastName,
    email: access!.email,
    phone: access!.phone,
    password: access!.password,
    birthDate: identity!.birthDate,
    docType: identity!.docType,
    docNumber: identity!.docNumber,
    nationality: identity!.nationality,
    country: identity!.country,
    address: identity!.address,
    city: identity!.city,
    postalCode: identity!.postalCode,
    incomeSource: financial!.incomeSource,
    annualIncome: financial!.annualIncome,
    netWorth: financial!.netWorth,
    riskTolerance: financial!.riskTolerance,
    tradingExperience: financial!.tradingExperience,
    investmentGoal: financial!.investmentGoal,
    isPep: compliance!.isPep,
    isFinraAffiliated: compliance!.isFinraAffiliated,
    acceptsTerms: compliance!.acceptsTerms,
    acceptsPrivacy: compliance!.acceptsPrivacy,
    authorizeDataUse: compliance!.authorizeDataUse,
    markets: [preferences!.markets],
    sectors: [preferences!.sectors],
    notifEmail: preferences!.notifEmail,
    notifSms: preferences!.notifSms,
    notifWhatsapp: preferences!.notifWhatsapp,
    monthlyReport: preferences!.monthlyReport,
    plan: plan!.plan,
    ...(plan!.plan === "premium" && plan!.billingCycle
      ? { billingCycle: plan!.billingCycle }
      : {}),
  };
}

export interface ConfirmPaymentResponse {
  message: string;
  plan: string;
}

export const registerService = {
  async checkEmail(email: string): Promise<{ available: boolean }> {
    const response = await fetch(
      `${BASE_URL}/auth/check-email?email=${encodeURIComponent(email)}`
    );
    if (!response.ok) throw new Error(`check-email returned ${response.status}`);
    return response.json();
  },

  async confirmPayment(sessionId: string): Promise<ConfirmPaymentResponse> {
    const response = await fetch(`${BASE_URL}/subscriptions/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sessionId }),
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({ error: "Error de conexión" }));
      throw new Error(body.error ?? "No se pudo confirmar el pago");
    }
    return response.json();
  },

  async submit(data: StoreData): Promise<RegisterResponse> {
    const payload = buildPayload(data);
    const response = await fetch(`${BASE_URL}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const body = await response.json().catch(() => ({ error: "Error de conexión" }));
      throw new Error(body.error ?? "Error al completar el registro");
    }

    return response.json();
  },
};
