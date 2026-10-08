import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, Eye, EyeOff, Loader2 } from "lucide-react";
import { Input } from "../../../../components/ui/Input";
import { Button } from "../../../../components/ui/Button";
import { WizardCard } from "../WizardCard";
import { step1Schema, type Step1Data } from "../../schemas/registerSchema";
import { useRegisterStore } from "../../store/registerStore";
import { registerService } from "../../services/registerService";

export function Step1Access() {
  const { goNext, setStepData, data } = useRegisterStore();
  const [showPwd, setShowPwd] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [emailTaken, setEmailTaken] = useState(false);
  const [checkingEmail, setCheckingEmail] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isValid },
  } = useForm<Step1Data>({
    resolver: zodResolver(step1Schema),
    mode: "onChange",
    defaultValues: data.access,
  });

  const emailValue = watch("email");

  useEffect(() => {
    setEmailTaken(false);
    if (!emailValue || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailValue)) return;

    const timer = setTimeout(async () => {
      setCheckingEmail(true);
      try {
        const { available } = await registerService.checkEmail(emailValue);
        setEmailTaken(!available);
      } catch (err) {
        console.error("check-email error:", err);
        setEmailTaken(false);
      } finally {
        setCheckingEmail(false);
      }
    }, 600);

    return () => clearTimeout(timer);
  }, [emailValue]);

  const onSubmit = (values: Step1Data) => {
    if (emailTaken) return;
    setStepData("access", values);
    goNext();
  };

  return (
    <WizardCard title="Datos de acceso" subtitle="Crea tus credenciales de ingreso">
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Nombre"
            placeholder="María"
            error={errors.firstName?.message}
            {...register("firstName")}
          />
          <Input
            label="Apellido"
            placeholder="Clavijo"
            error={errors.lastName?.message}
            {...register("lastName")}
          />
        </div>

        <Input
          label="Correo electrónico"
          type="email"
          placeholder="maria@ejemplo.com"
          autoComplete="email"
          error={emailTaken ? "Este correo ya está registrado" : errors.email?.message}
          rightIcon={
            checkingEmail
              ? <Loader2 size={14} className="animate-spin text-text-muted" />
              : undefined
          }
          {...register("email")}
        />

        <Input
          label="Teléfono / Celular"
          type="tel"
          placeholder="+57 300 000 0000"
          error={errors.phone?.message}
          {...register("phone")}
        />

        <Input
          label="Contraseña"
          type={showPwd ? "text" : "password"}
          placeholder="••••••••"
          autoComplete="new-password"
          error={errors.password?.message}
          rightIcon={
            <button
              type="button"
              tabIndex={-1}
              onClick={() => setShowPwd((v) => !v)}
              className="text-text-subtle hover:text-text-secondary transition-colors"
            >
              {showPwd ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          }
          {...register("password")}
        />

        <div>
          <Input
            label="Confirmar contraseña"
            type={showConfirm ? "text" : "password"}
            placeholder="••••••••"
            autoComplete="new-password"
            error={errors.confirmPassword?.message}
            rightIcon={
              <button
                type="button"
                tabIndex={-1}
                onClick={() => setShowConfirm((v) => !v)}
                className="text-text-subtle hover:text-text-secondary transition-colors"
              >
                {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            }
            {...register("confirmPassword")}
          />
          <p className="mt-1 text-xs text-text-muted">
            Mínimo 8 caracteres, 1 mayúscula, 1 número y 1 símbolo.
          </p>
        </div>

        <div className="flex justify-end mt-2">
          <Button
            type="submit"
            variant="primary"
            disabled={!isValid || emailTaken || checkingEmail}
            rightIcon={<ArrowRight size={16} />}
          >
            Continuar
          </Button>
        </div>
      </form>
    </WizardCard>
  );
}
