const BASE_URL = "http://localhost:8081/api/v1/subscriptions";

function authHeaders(token: string) {
  return { "Content-Type": "application/json", Authorization: `Bearer ${token}` };
}

export const subscriptionService = {
  async checkout(billingCycle: "monthly" | "annual", token: string): Promise<string> {
    const res = await fetch(`${BASE_URL}/checkout`, {
      method: "POST",
      headers: authHeaders(token),
      body: JSON.stringify({ billingCycle }),
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.error ?? "No se pudo iniciar el proceso de suscripción.");
    if (!body.checkoutUrl) throw new Error("Stripe no está configurado. Contacta al administrador.");
    return body.checkoutUrl;
  },

  async cancelSubscription(token: string): Promise<void> {
    const res = await fetch(`${BASE_URL}/cancel`, {
      method: "POST",
      headers: authHeaders(token),
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.error ?? "No se pudo cancelar la suscripción.");
    }
  },
};
