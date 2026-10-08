import { useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { Button } from "../../../../components/ui/Button";
import { WizardCard } from "../WizardCard";
import { PlanCard } from "../shared/PlanCard";
import { step6Schema, type Step6Data } from "../../schemas/registerSchema";
import { useRegisterStore } from "../../store/registerStore";
import { registerService } from "../../services/registerService";
import { cn } from "../../../../lib/cn";

export function Step6Plan() {
  const { goBack, data, reset } = useRegisterStore();
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState("");

  const {
    control,
    handleSubmit,
    watch,
    setValue,
    formState: { isValid, isSubmitting, errors },
  } = useForm<Step6Data>({
    resolver: zodResolver(step6Schema),
    mode: "onChange",
    defaultValues: data.plan ?? { plan: undefined as unknown as "basic" | "premium" },
  });

  const selectedPlan = watch("plan");
  const selectedCycle = watch("billingCycle");

  const onSubmit = async (values: Step6Data) => {
    setSubmitError("");
    try {
      const result = await registerService.submit({ ...data, plan: values });
      reset();
      if (result.checkoutUrl) {
        window.location.href = result.checkoutUrl;
      } else {
        navigate("/login", {
          state: { successMessage: "Cuenta creada exitosamente. Inicia sesión para continuar." },
        });
      }
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "Error al completar el registro. Intenta de nuevo.");
    }
  };

  return (
    <WizardCard title="Plan y cuenta" subtitle="Elige tu plan e integra tu cuenta de fondos">
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
        <p className="text-xs uppercase tracking-wide text-text-muted font-medium">
          Selecciona tu plan
        </p>

        <Controller
          control={control}
          name="plan"
          render={() => (
            <div className="grid grid-cols-2 gap-4">
              <PlanCard
                plan="basic"
                selected={selectedPlan === "basic"}
                onSelect={() => setValue("plan", "basic", { shouldValidate: true })}
              />
              <PlanCard
                plan="premium"
                selected={selectedPlan === "premium"}
                onSelect={() => setValue("plan", "premium", { shouldValidate: true })}
              />
            </div>
          )}
        />

        {selectedPlan === "premium" && (
          <div>
            <p className="text-xs uppercase tracking-wide text-text-muted font-medium mb-3">
              Ciclo de facturación
            </p>
            <Controller
              control={control}
              name="billingCycle"
              render={({ field }) => (
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => field.onChange("monthly")}
                    className={cn(
                      "rounded-xl p-4 border text-left transition-colors",
                      selectedCycle === "monthly"
                        ? "border-gold bg-gold-subtle"
                        : "border-border-default bg-bg-tertiary hover:border-gold/40"
                    )}
                  >
                    <p className="text-sm font-semibold text-text-secondary">Mensual</p>
                    <p className="text-xs text-text-muted">$12 / mes</p>
                  </button>
                  <button
                    type="button"
                    onClick={() => field.onChange("annual")}
                    className={cn(
                      "rounded-xl p-4 border text-left transition-colors",
                      selectedCycle === "annual"
                        ? "border-gold bg-gold-subtle"
                        : "border-border-default bg-bg-tertiary hover:border-gold/40"
                    )}
                  >
                    <p className="text-sm font-semibold text-text-secondary">Anual</p>
                    <p className="text-xs text-text-muted">$120 / año · 2 meses gratis</p>
                  </button>
                </div>
              )}
            />
            {errors.billingCycle && (
              <p className="text-xs text-danger mt-1">{errors.billingCycle.message}</p>
            )}
          </div>
        )}

        <div className="bg-bg-tertiary rounded-xl p-4 border border-border-subtle">
          <div className="flex items-center gap-2 mb-2">
            <span className="w-2 h-2 rounded-full bg-gold shrink-0" />
            <p className="text-sm font-semibold text-text-secondary">
              Integración con Alpaca
            </p>
          </div>
          <p className="text-xs text-text-muted">
            Acciones ElBosque utiliza Alpaca como broker-dealer para la custodia y
            ejecución. Al completar el registro se creará automáticamente tu cuenta
            de fondos.
          </p>
        </div>

        {submitError && (
          <p className="text-sm text-danger">{submitError}</p>
        )}

        <div className="flex justify-between mt-1">
          <Button
            type="button"
            variant="secondary"
            leftIcon={<ArrowLeft size={16} />}
            onClick={goBack}
            disabled={isSubmitting}
          >
            Atrás
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={!isValid}
            loading={isSubmitting}
            rightIcon={<ArrowRight size={16} />}
          >
            Completar registro
          </Button>
        </div>
      </form>
    </WizardCard>
  );
}
