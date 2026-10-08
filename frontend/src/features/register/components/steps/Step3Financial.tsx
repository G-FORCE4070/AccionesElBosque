import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Select } from "../../../../components/ui/Select";
import { Button } from "../../../../components/ui/Button";
import { WizardCard } from "../WizardCard";
import { ChipGroup } from "../shared/ChipGroup";
import { step3Schema, type Step3Data } from "../../schemas/registerSchema";
import { useRegisterStore } from "../../store/registerStore";

const INCOME_SOURCES = [
  { value: "salary", label: "Salario" },
  { value: "business", label: "Negocio propio" },
  { value: "investments", label: "Inversiones" },
  { value: "pension", label: "Pensión" },
  { value: "other", label: "Otro" },
];

const INVESTMENT_GOALS = [
  { value: "preservation", label: "Preservación de capital" },
  { value: "income", label: "Ingreso periódico" },
  { value: "growth", label: "Crecimiento a largo plazo" },
  { value: "speculation", label: "Especulación a corto plazo" },
];

const ANNUAL_INCOME_OPTS = [
  { value: "<30k", label: "Menos de $30k" },
  { value: "30k-100k", label: "$30k – $100k" },
  { value: "100k-500k", label: "$100k – $500k" },
  { value: ">500k", label: "Más de $500k" },
];

const NET_WORTH_OPTS = [
  { value: "<50k", label: "Menos de $50k" },
  { value: "50k-250k", label: "$50k – $250k" },
  { value: ">250k", label: "Más de $250k" },
];

const RISK_OPTS = [
  { value: "conservative", label: "Conservador" },
  { value: "moderate", label: "Moderado" },
  { value: "aggressive", label: "Agresivo" },
];

const EXPERIENCE_OPTS = [
  { value: "beginner", label: "Principiante" },
  { value: "experienced", label: "Con experiencia" },
];

function FieldLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-xs uppercase tracking-wide text-text-muted font-medium mb-2">
      {children}
    </p>
  );
}

export function Step3Financial() {
  const { goNext, goBack, setStepData, data } = useRegisterStore();

  const {
    control,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isValid },
  } = useForm<Step3Data>({
    resolver: zodResolver(step3Schema),
    mode: "onChange",
    defaultValues: data.financial ?? {
      incomeSource: "",
      annualIncome: "",
      netWorth: "",
      riskTolerance: "",
      tradingExperience: "",
      investmentGoal: "",
    },
  });

  const incomeSource = watch("incomeSource");
  const investmentGoal = watch("investmentGoal");

  const onSubmit = (values: Step3Data) => {
    setStepData("financial", values);
    goNext();
  };

  return (
    <WizardCard
      title="Perfil financiero"
      subtitle="Nos ayuda a personalizar tu experiencia"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
        <Select
          label="Fuente de ingresos"
          options={INCOME_SOURCES}
          value={incomeSource}
          onChange={(v) => setValue("incomeSource", v, { shouldValidate: true })}
          placeholder="Seleccione..."
        />
        {errors.incomeSource && (
          <p className="text-xs text-danger -mt-4">{errors.incomeSource.message}</p>
        )}

        <div>
          <FieldLabel>Ingreso anual (USD)</FieldLabel>
          <Controller
            control={control}
            name="annualIncome"
            render={({ field }) => (
              <ChipGroup
                options={ANNUAL_INCOME_OPTS}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
          {errors.annualIncome && (
            <p className="text-xs text-danger mt-1">{errors.annualIncome.message}</p>
          )}
        </div>

        <div>
          <FieldLabel>Patrimonio total (USD)</FieldLabel>
          <Controller
            control={control}
            name="netWorth"
            render={({ field }) => (
              <ChipGroup
                options={NET_WORTH_OPTS}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
          {errors.netWorth && (
            <p className="text-xs text-danger mt-1">{errors.netWorth.message}</p>
          )}
        </div>

        <div>
          <FieldLabel>Tolerancia al riesgo</FieldLabel>
          <Controller
            control={control}
            name="riskTolerance"
            render={({ field }) => (
              <ChipGroup
                options={RISK_OPTS}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
          {errors.riskTolerance && (
            <p className="text-xs text-danger mt-1">{errors.riskTolerance.message}</p>
          )}
        </div>

        <div>
          <FieldLabel>Experiencia en trading</FieldLabel>
          <Controller
            control={control}
            name="tradingExperience"
            render={({ field }) => (
              <ChipGroup
                options={EXPERIENCE_OPTS}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
          {errors.tradingExperience && (
            <p className="text-xs text-danger mt-1">{errors.tradingExperience.message}</p>
          )}
        </div>

        <Select
          label="Objetivo de inversión"
          options={INVESTMENT_GOALS}
          value={investmentGoal}
          onChange={(v) => setValue("investmentGoal", v, { shouldValidate: true })}
          placeholder="Seleccione..."
        />
        {errors.investmentGoal && (
          <p className="text-xs text-danger -mt-4">{errors.investmentGoal.message}</p>
        )}

        <div className="flex justify-between mt-2">
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
