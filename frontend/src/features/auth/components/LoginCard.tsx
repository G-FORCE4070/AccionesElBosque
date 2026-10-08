import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff, ArrowRight } from "lucide-react";
import { useNavigate, Link } from "react-router-dom";
import { Button } from "../../../components/ui/Button";
import { Input } from "../../../components/ui/Input";
import { loginSchema, type LoginFormData } from "../schemas/loginSchema";
import { authService } from "../services/authService";

export function LoginCard() {
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState("");
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setServerError("");
    try {
      const result = await authService.login(data.email, data.password);
      navigate("/mfa", { state: { sessionId: result.sessionId, email: data.email } });
    } catch (err) {
      setServerError(err instanceof Error ? err.message : "Error al iniciar sesión");
    }
  };

  return (
    <div className="bg-bg-secondary rounded-2xl p-8 border border-gold/30 w-[380px]">
      <h2 className="font-display text-3xl text-text-primary">Acceso seguro</h2>
      <p className="mt-1 text-sm text-text-muted">Bienvenido de vuelta a su terminal</p>

      <form onSubmit={handleSubmit(onSubmit)} className="mt-7 flex flex-col gap-4">
        <Input
          label="Correo electrónico"
          placeholder="inversor@unbosque.edu.co"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...register("email")}
        />

        <div>
          <div className="flex justify-between items-center mb-2">
            <span className="text-xs uppercase tracking-wide text-text-muted font-medium">
              Contraseña
            </span>
            <a href="#" className="text-xs text-gold underline">
              ¿Olvidó su contraseña?
            </a>
          </div>
          <Input
            type={showPassword ? "text" : "password"}
            placeholder="••••••••"
            autoComplete="current-password"
            error={errors.password?.message}
            rightIcon={
              <button
                type="button"
                onClick={() => setShowPassword(v => !v)}
                className="text-text-subtle hover:text-text-secondary transition-colors"
                tabIndex={-1}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            }
            {...register("password")}
          />
        </div>

        {serverError && (
          <p className="text-xs text-danger">{serverError}</p>
        )}

        <Button
          type="submit"
          variant="primary"
          size="md"
          loading={isSubmitting}
          rightIcon={<ArrowRight size={16} />}
          className="w-full mt-2"
        >
          Iniciar sesión
        </Button>
      </form>

      <p className="mt-4 text-center text-xs text-text-muted">
        ¿Primera vez en Acciones ElBosque?{" "}
        <Link to="/register" className="text-gold underline">
          Registrarse
        </Link>
      </p>
    </div>
  );
}
