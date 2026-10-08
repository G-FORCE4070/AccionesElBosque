import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { Input } from "../../../../components/ui/Input";
import { Select } from "../../../../components/ui/Select";
import { Button } from "../../../../components/ui/Button";
import { WizardCard } from "../WizardCard";
import { step2Schema, type Step2Data } from "../../schemas/registerSchema";
import { useRegisterStore } from "../../store/registerStore";

const DOC_TYPES = [
  { value: "cc", label: "Cédula de Ciudadanía" },
  { value: "ce", label: "Cédula de Extranjería" },
  { value: "passport", label: "Pasaporte" },
];

const NATIONALITIES = [
  { value: "CO", label: "Colombiana/o" },
  { value: "US", label: "Estadounidense" },
  { value: "MX", label: "Mexicana/o" },
  { value: "AR", label: "Argentina/o" },
  { value: "BR", label: "Brasileña/o" },
  { value: "CL", label: "Chilena/o" },
  { value: "PE", label: "Peruana/o" },
  { value: "VE", label: "Venezolana/o" },
  { value: "EC", label: "Ecuatoriana/o" },
  { value: "BO", label: "Boliviana/o" },
  { value: "PY", label: "Paraguaya/o" },
  { value: "UY", label: "Uruguaya/o" },
  { value: "PA", label: "Panameña/o" },
  { value: "CR", label: "Costarricense" },
  { value: "GT", label: "Guatemalteca/o" },
  { value: "DO", label: "Dominicana/o" },
  { value: "ES", label: "Española/ol" },
  { value: "GB", label: "Británica/o" },
  { value: "DE", label: "Alemana/o" },
  { value: "FR", label: "Francesa/és" },
  { value: "CA", label: "Canadiense" },
  { value: "AU", label: "Australiana/o" },
];

const COUNTRIES = [
  { value: "CO", label: "Colombia" },
  { value: "US", label: "Estados Unidos" },
  { value: "MX", label: "México" },
  { value: "AR", label: "Argentina" },
  { value: "BR", label: "Brasil" },
  { value: "CL", label: "Chile" },
  { value: "PE", label: "Perú" },
  { value: "VE", label: "Venezuela" },
  { value: "EC", label: "Ecuador" },
  { value: "BO", label: "Bolivia" },
  { value: "PY", label: "Paraguay" },
  { value: "UY", label: "Uruguay" },
  { value: "PA", label: "Panamá" },
  { value: "CR", label: "Costa Rica" },
  { value: "GT", label: "Guatemala" },
  { value: "DO", label: "República Dominicana" },
  { value: "ES", label: "España" },
  { value: "GB", label: "Reino Unido" },
  { value: "DE", label: "Alemania" },
  { value: "FR", label: "Francia" },
  { value: "CA", label: "Canadá" },
  { value: "AU", label: "Australia" },
];

export function Step2Identity() {
  const { goNext, goBack, setStepData, data } = useRegisterStore();

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isValid },
  } = useForm<Step2Data>({
    resolver: zodResolver(step2Schema),
    mode: "onChange",
    defaultValues: data.identity,
  });

  const docType = watch("docType");
  const nationality = watch("nationality");
  const country = watch("country");

  const onSubmit = (values: Step2Data) => {
    setStepData("identity", values);
    goNext();
  };

  return (
    <WizardCard title="Identidad" subtitle="Requerido por regulaciones KYC">
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        <Input
          label="Fecha de nacimiento"
          type="date"
          error={errors.birthDate?.message}
          {...register("birthDate")}
        />

        <Select
          label="Tipo de documento"
          options={DOC_TYPES}
          value={docType}
          onChange={(v) => setValue("docType", v, { shouldValidate: true })}
          placeholder="Seleccione..."
        />

        {errors.docType && (
          <p className="text-xs text-danger -mt-3">{errors.docType.message}</p>
        )}

        <Input
          label="Número de identificación"
          placeholder="1.234.567.890"
          error={errors.docNumber?.message}
          {...register("docNumber")}
        />

        <div className="grid grid-cols-2 gap-4">
          <Select
            label="Nacionalidad"
            options={NATIONALITIES}
            value={nationality}
            onChange={(v) => setValue("nationality", v, { shouldValidate: true })}
            placeholder="Seleccione..."
          />
          <Select
            label="País de residencia fiscal"
            options={COUNTRIES}
            value={country}
            onChange={(v) => setValue("country", v, { shouldValidate: true })}
            placeholder="Seleccione..."
          />
        </div>

        {(errors.nationality || errors.country) && (
          <p className="text-xs text-danger -mt-3">
            {errors.nationality?.message ?? errors.country?.message}
          </p>
        )}

        <Input
          label="Dirección completa"
          placeholder="Calle 123 # 45-67"
          error={errors.address?.message}
          {...register("address")}
        />

        <div className="grid grid-cols-2 gap-4">
          <Input
            label="Ciudad"
            placeholder="Bogotá"
            error={errors.city?.message}
            {...register("city")}
          />
          <Input
            label="Código postal"
            placeholder="110111"
            error={errors.postalCode?.message}
            {...register("postalCode")}
          />
        </div>

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
