import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight, AlertTriangle } from "lucide-react";
import { Checkbox } from "../../../../components/ui/Checkbox";
import { Button } from "../../../../components/ui/Button";
import { WizardCard } from "../WizardCard";
import { step4Schema, type Step4Data } from "../../schemas/registerSchema";
import { useRegisterStore } from "../../store/registerStore";

export function Step4Compliance() {
  const { goNext, goBack, setStepData, data } = useRegisterStore();

  const {
    control,
    handleSubmit,
    formState: { isValid },
  } = useForm<Step4Data>({
    resolver: zodResolver(step4Schema),
    mode: "onChange",
    defaultValues: data.compliance ?? {
      isPep: false,
      isFinraAffiliated: false,
      acceptsTerms: false,
      acceptsPrivacy: false,
      authorizeDataUse: false,
    },
  });

  const onSubmit = (values: Step4Data) => {
    setStepData("compliance", values);
    goNext();
  };

  return (
    <WizardCard title="Cumplimiento" subtitle="Requerido por normativa financiera">
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
        {/* Warning banner */}
        <div className="flex gap-3 bg-warning/10 border-l-[3px] border-warning rounded-lg p-4">
          <AlertTriangle size={18} className="text-warning shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-semibold text-text-secondary">
              Cumplimiento KYC / AML
            </p>
            <p className="mt-1 text-xs text-text-muted">
              Como entidad financiera regulada, estamos obligados a verificar tu
              identidad conforme a normativas internacionales anti-lavado de activos.
            </p>
          </div>
        </div>

        {/* Checkboxes */}
        <div className="flex flex-col gap-4">
          <Controller
            control={control}
            name="isPep"
            render={({ field }) => (
              <div className="flex items-start gap-3">
                <Checkbox
                  checked={field.value}
                  onChange={field.onChange}
                  className="mt-0.5"
                />
                <p className="text-sm text-text-secondary leading-snug">
                  Soy una{" "}
                  <span className="font-semibold text-gold cursor-pointer underline">
                    persona políticamente expuesta (PEP)
                  </span>{" "}
                  o familiar cercano de una
                </p>
              </div>
            )}
          />

          <Controller
            control={control}
            name="isFinraAffiliated"
            render={({ field }) => (
              <div className="flex items-start gap-3">
                <Checkbox
                  checked={field.value}
                  onChange={field.onChange}
                  className="mt-0.5"
                />
                <p className="text-sm text-text-secondary leading-snug">
                  Estoy afiliado o trabajo en una{" "}
                  <span className="font-semibold text-gold cursor-pointer underline">
                    bolsa de valores o FINRA
                  </span>
                </p>
              </div>
            )}
          />

          <Controller
            control={control}
            name="acceptsTerms"
            render={({ field }) => (
              <div className="flex items-start gap-3">
                <Checkbox
                  checked={field.value}
                  onChange={field.onChange}
                  className="mt-0.5"
                />
                <p className="text-sm text-text-secondary leading-snug">
                  He leído y acepto los{" "}
                  <span className="font-semibold text-gold cursor-pointer underline">
                    Términos y Condiciones
                  </span>
                  <span className="text-danger ml-1">*</span>
                </p>
              </div>
            )}
          />

          <Controller
            control={control}
            name="acceptsPrivacy"
            render={({ field }) => (
              <div className="flex items-start gap-3">
                <Checkbox
                  checked={field.value}
                  onChange={field.onChange}
                  className="mt-0.5"
                />
                <p className="text-sm text-text-secondary leading-snug">
                  Acepto la{" "}
                  <span className="font-semibold text-gold cursor-pointer underline">
                    Política de Privacidad
                  </span>
                  <span className="text-danger ml-1">*</span>
                </p>
              </div>
            )}
          />

          <Controller
            control={control}
            name="authorizeDataUse"
            render={({ field }) => (
              <div className="flex items-start gap-3">
                <Checkbox
                  checked={field.value}
                  onChange={field.onChange}
                  className="mt-0.5"
                />
                <div>
                  <p className="text-sm text-text-secondary leading-snug">
                    Autorizo el uso de mis datos para{" "}
                    <span className="font-semibold text-gold cursor-pointer underline">
                      análisis de riesgo
                    </span>
                  </p>
                  <p className="text-xs text-text-muted mt-0.5">(Opcional)</p>
                </div>
              </div>
            )}
          />
        </div>

        <p className="text-xs text-text-subtle">
          <span className="text-danger">*</span> Campos obligatorios
        </p>

        <div className="flex justify-between mt-1">
          <Button
            type="button"
            variant="secondary"
            leftIcon={<ArrowLeft size={16} />}
            onClick={goBack}
          >
            Atrás
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={!isValid}
            rightIcon={<ArrowRight size={16} />}
          >
            Continuar
          </Button>
        </div>
      </form>
    </WizardCard>
  );
}
