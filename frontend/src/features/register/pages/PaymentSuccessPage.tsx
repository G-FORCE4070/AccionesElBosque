import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { CheckCircle, XCircle, Loader2 } from "lucide-react";
import { Button } from "../../../components/ui/Button";
import { Logo } from "../../../components/ui/Logo";
import { registerService } from "../services/registerService";

type Status = "loading" | "success" | "error";

export function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<Status>("loading");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    const sessionId = searchParams.get("session_id");
    if (!sessionId) {
      setErrorMessage("No se encontró el identificador de sesión de pago.");
      setStatus("error");
      return;
    }

    registerService
      .confirmPayment(sessionId)
      .then(() => setStatus("success"))
      .catch((err) => {
        setErrorMessage(err instanceof Error ? err.message : "Error al confirmar el pago.");
        setStatus("error");
      });
  }, [searchParams]);

  return (
    <div className="min-h-screen bg-bg-primary flex flex-col items-center justify-center p-6">
      <div className="w-full max-w-md bg-bg-secondary border border-border-subtle rounded-2xl p-8 flex flex-col items-center gap-6 text-center">
        <Logo iconSize={28} />

        {status === "loading" && (
          <>
            <Loader2 size={48} className="text-gold animate-spin" />
            <div>
              <p className="text-lg font-semibold text-text-primary">Confirmando pago…</p>
              <p className="text-sm text-text-muted mt-1">Verificando tu suscripción con Stripe.</p>
            </div>
          </>
        )}

        {status === "success" && (
          <>
            <CheckCircle size={48} className="text-success" />
            <div>
              <p className="text-lg font-semibold text-text-primary">¡Pago confirmado!</p>
              <p className="text-sm text-text-muted mt-1">
                Tu cuenta Premium está activa. Ya puedes iniciar sesión.
              </p>
            </div>
            <Button
              variant="primary"
              onClick={() =>
                navigate("/login", {
                  state: { successMessage: "Cuenta Premium activada. Inicia sesión para continuar." },
                })
              }
            >
              Ir al inicio de sesión
            </Button>
          </>
        )}

        {status === "error" && (
          <>
            <XCircle size={48} className="text-danger" />
            <div>
              <p className="text-lg font-semibold text-text-primary">No se pudo confirmar el pago</p>
              <p className="text-sm text-text-muted mt-1">{errorMessage}</p>
            </div>
            <div className="flex gap-3 w-full">
              <Button
                variant="secondary"
                onClick={() => window.location.reload()}
                className="flex-1"
              >
                Reintentar
              </Button>
              <Button
                variant="primary"
                onClick={() => navigate("/login")}
                className="flex-1"
              >
                Ir al login
              </Button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
