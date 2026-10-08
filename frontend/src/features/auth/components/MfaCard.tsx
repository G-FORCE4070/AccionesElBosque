import { useState, useEffect } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { Logo } from "../../../components/ui/Logo";
import { Button } from "../../../components/ui/Button";
import { OtpInput } from "./OtpInput";
import { authService } from "../services/authService";
import { useSessionStore } from "../../../store/sessionStore";

function maskEmail(email: string): string {
  const [name, domain] = email.split("@");
  if (!domain) return email;
  if (name.length <= 2) return email;
  return `${name[0]}***${name[name.length - 1]}@${domain}`;
}

export function MfaCard() {
  const location = useLocation();
  const sessionId: string = location.state?.sessionId ?? "";
  const email: string = location.state?.email ?? "";

  const [digits, setDigits] = useState<string[]>(Array(6).fill(""));
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);
  const [resendNotice, setResendNotice] = useState(false);
  const [sessionExpired, setSessionExpired] = useState(false);

  const navigate = useNavigate();
  const setSession = useSessionStore(s => s.setSession);

  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setTimeout(() => setResendCooldown(c => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendCooldown]);

  const verify = async (code: string) => {
    setError("");
    setIsLoading(true);
    try {
      const result = await authService.verifyMfa(sessionId, code);
      setSession(result.token, result.user);
      navigate("/dashboard");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Error de verificación";
      if (message === "El código ha expirado") {
        setSessionExpired(true);
      } else {
        setError(message);
      }
      setDigits(Array(6).fill(""));
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = () => {
    const code = digits.join("");
    if (code.length < 6) {
      setError("El código debe tener 6 dígitos");
      return;
    }
    verify(code);
  };

  const handleResend = async () => {
    try {
      await authService.resendMfa(sessionId);
      setResendCooldown(30);
      setResendNotice(true);
      setTimeout(() => setResendNotice(false), 3000);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Error al reenviar";
      if (message === "El código ha expirado") {
        setSessionExpired(true);
      } else {
        setError(message);
      }
    }
  };

  if (sessionExpired) {
    return (
      <div className="bg-bg-secondary rounded-2xl p-10 border border-gold/20 w-[420px] text-center">
        <div className="flex justify-center mb-6">
          <Logo iconSize={28} />
        </div>
        <h2 className="font-display text-2xl text-text-primary mb-3">Código expirado</h2>
        <p className="text-sm text-text-muted mb-6">
          Tu código de verificación ha expirado o ya no es válido.
          Por favor, inicia sesión nuevamente.
        </p>
        <Link
          to="/login"
          className="inline-block px-6 py-2.5 rounded-xl bg-gold text-bg-primary text-sm font-semibold hover:opacity-90 transition-opacity"
        >
          Volver al login
        </Link>
      </div>
    );
  }

  const masked = maskEmail(email);

  return (
    <div className="bg-bg-secondary rounded-2xl p-10 border border-gold/20 w-[420px]">
      <div className="flex justify-center">
        <Logo iconSize={28} />
      </div>

      <div className="mt-6 text-center">
        <h2 className="font-display text-3xl text-text-primary">Verificación MFA</h2>
        <p className="mt-2 text-sm text-text-muted">
          Ingresa el código de 6 dígitos que enviamos a
        </p>
        <p className="text-sm text-text-secondary font-medium">{masked}</p>
      </div>

      <div className="mt-8">
        <OtpInput
          value={digits}
          onChange={setDigits}
          onComplete={verify}
          error={!!error}
        />
      </div>

      {error && (
        <p className="mt-3 text-center text-sm text-danger">{error}</p>
      )}

      {resendNotice && (
        <p className="mt-3 text-center text-sm text-success">Código reenviado</p>
      )}

      <Button
        variant="primary"
        size="md"
        loading={isLoading}
        rightIcon={<ArrowRight size={16} />}
        onClick={handleSubmit}
        className="w-full mt-6"
      >
        Verificar código
      </Button>

      <div className="mt-4 flex justify-between items-center">
        <button
          type="button"
          onClick={() => navigate("/login")}
          className="text-sm text-text-muted hover:text-text-secondary transition-colors"
        >
          ← Cancelar
        </button>
        <button
          type="button"
          onClick={handleResend}
          disabled={resendCooldown > 0}
          className="text-sm text-gold underline disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
        >
          {resendCooldown > 0 ? `Reenviar (${resendCooldown}s)` : "Reenviar código"}
        </button>
      </div>
    </div>
  );
}
