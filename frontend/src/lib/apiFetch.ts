import { useSessionStore } from "../store/sessionStore";

export async function apiFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const token = useSessionStore.getState().token;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(init.headers as Record<string, string>),
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(input, { ...init, headers });

  if (response.status === 401) {
    useSessionStore.getState().clearSession();
    // BrowserRouter does not expose a standalone navigate; pass message via sessionStorage.
    sessionStorage.setItem("aeb_redirect_msg", "Tu sesión ha expirado. Inicia sesión nuevamente.");
    window.location.href = "/login";
    throw new Error("Tu sesión ha expirado. Inicia sesión nuevamente.");
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.error ?? "Error inesperado del servidor");
  }

  return response;
}
