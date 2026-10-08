import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Toggle } from "../../../../components/ui/Toggle";
import { Button } from "../../../../components/ui/Button";
import { WizardCard } from "../WizardCard";
import { ChipGroup } from "../shared/ChipGroup";
import { step5Schema, type Step5Data } from "../../schemas/registerSchema";
import { useRegisterStore } from "../../store/registerStore";

const MARKET_OPTS = [
  { value: "nyse", label: "NYSE" },
  { value: "nasdaq", label: "NASDAQ" },
  { value: "lse", label: "LSE (Londres)", disabled: true },
  { value: "tse", label: "TSE (Tokio)", disabled: true },
  { value: "asx", label: "ASX (Sídney)", disabled: true },
];

const SECTOR_OPTS = [
  { value: "tech", label: "Tecnología" },
  { value: "health", label: "Salud" },
  { value: "consumer", label: "Consumo" },
];

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-xs uppercase tracking-wide text-text-muted font-medium mb-2">
      {children}
    </p>
  );
}

function ToggleRow({
  label,
  description,
  checked,
  onChange,
  disabled = false,
}: {
  label: string;
  description: string;
  checked: boolean;
  onChange: (v: boolean) => void;
  disabled?: boolean;
}) {
  return (
    <div className={`flex justify-between items-start py-3 border-b border-border-subtle/50 ${disabled ? "opacity-40 cursor-not-allowed" : ""}`}>
      <div className="pr-4">
        <p className="text-sm text-text-secondary font-medium">{label}</p>
        <p className="text-xs text-text-muted mt-0.5">{description}{disabled ? " — Próximamente" : ""}</p>
      </div>
      <Toggle checked={checked} onChange={disabled ? () => {} : onChange} className="shrink-0 mt-0.5" />
    </div>
  );
}

export function Step5Preferences() {
  const { goNext, goBack, setStepData, data } = useRegisterStore();

  const {
    control,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<Step5Data>({
    resolver: zodResolver(step5Schema),
    mode: "onChange",
    defaultValues: data.preferences ?? {
      markets: "",
      sectors: "",
      notifEmail: true,
      notifSms: false,
      notifWhatsapp: false,
      monthlyReport: false,
    },
  });

  const onSubmit = (values: Step5Data) => {
    setStepData("preferences", values);
    goNext();
  };

  return (
    <WizardCard
      title="Preferencias"
      subtitle="Personaliza tu experiencia de inversión"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5">
        <div>
          <SectionLabel>Mercado de interés</SectionLabel>
          <Controller
            control={control}
            name="markets"
            render={({ field }) => (
              <ChipGroup
                options={MARKET_OPTS}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
          {errors.markets && (
            <p className="text-xs text-danger mt-1">{errors.markets.message}</p>
          )}
        </div>

        <div>
          <SectionLabel>Sector de interés</SectionLabel>
          <Controller
            control={control}
            name="sectors"
            render={({ field }) => (
              <ChipGroup
                options={SECTOR_OPTS}
                value={field.value}
                onChange={field.onChange}
              />
            )}
          />
          {errors.sectors && (
            <p className="text-xs text-danger mt-1">{errors.sectors.message}</p>
          )}
        </div>

        <div>
          <SectionLabel>Comisionista</SectionLabel>
          <div className="flex items-center gap-2 rounded-lg border border-border-default bg-bg-tertiary px-3 py-2.5 text-sm text-text-muted/60 cursor-not-allowed select-none">
            Sin comisionista asignado
          </div>
          <p className="mt-1 text-xs text-text-muted">
            La asignación de comisionistas no está disponible en el MVP.
          </p>
        </div>

        <div>
          <SectionLabel>Notificaciones y reportes</SectionLabel>
          <Controller
            control={control}
            name="notifEmail"
            render={({ field }) => (
              <ToggleRow
                label="Notificaciones por email"
                description="Alertas de mercado y resúmenes diarios"
                checked={field.value}
                onChange={field.onChange}
              />
            )}
          />
          <Controller
            control={control}
            name="notifSms"
            render={({ field }) => (
              <ToggleRow
                label="Notificaciones por SMS"
                description="Alertas críticas de ejecución de órdenes"
                checked={field.value}
                onChange={field.onChange}
                disabled
              />
            )}
          />
          <Controller
            control={control}
            name="notifWhatsapp"
            render={({ field }) => (
              <ToggleRow
                label="Notificaciones por WhatsApp"
                description="Confirmaciones y alertas vía WhatsApp"
                checked={field.value}
                onChange={field.onChange}
                disabled
              />
            )}
          />
          <Controller
            control={control}
            name="monthlyReport"
            render={({ field }) => (
              <ToggleRow
                label="Reporte mensual automático"
                description="Recibe un resumen de ganancias y comisiones desglosadas"
                checked={field.value}
                onChange={field.onChange}
                disabled
              />
            )}
          />
        </div>

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
