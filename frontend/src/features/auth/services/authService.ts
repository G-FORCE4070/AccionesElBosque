const BASE_URL = "http://localhost:8081/api/v1/auth";

export const authService = {
  async login(email: string, password: string): Promise<{ sessionId: string }> {
    const response = await fetch(`${BASE_URL}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    const body = await response.json();
    if (!response.ok) throw new Error(body.error ?? "Error al iniciar sesión");
    return body;
  },

  async verifyMfa(sessionId: string, code: string): Promise<{ token: string; user: import("../../../store/sessionStore").User }> {
    const response = await fetch(`${BASE_URL}/mfa/verify`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sessionId, code }),
    });
    const body = await response.json();
    if (!response.ok) throw new Error(body.error ?? "Error al verificar código");
    return body;
  },

  async resendMfa(sessionId: string): Promise<void> {
    const response = await fetch(`${BASE_URL}/mfa/resend`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ sessionId }),
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.error ?? "Error al reenviar código");
    }
  },
};
