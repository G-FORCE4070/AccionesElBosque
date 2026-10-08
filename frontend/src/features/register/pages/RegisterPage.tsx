import { Link } from "react-router-dom";
import { AuthShell } from "../../auth/components/AuthShell";
import { Logo } from "../../../components/ui/Logo";
import { StepIndicator } from "../components/StepIndicator";
import { Step1Access } from "../components/steps/Step1Access";
import { Step2Identity } from "../components/steps/Step2Identity";
import { Step3Financial } from "../components/steps/Step3Financial";
import { Step4Compliance } from "../components/steps/Step4Compliance";
import { Step5Preferences } from "../components/steps/Step5Preferences";
import { Step6Plan } from "../components/steps/Step6Plan";
import { useRegisterStore } from "../store/registerStore";

const STEP_COMPONENTS = [
  <Step1Access key={1} />,
  <Step2Identity key={2} />,
  <Step3Financial key={3} />,
  <Step4Compliance key={4} />,
  <Step5Preferences key={5} />,
  <Step6Plan key={6} />,
];

export function RegisterPage() {
  const currentStep = useRegisterStore((s) => s.currentStep);

  return (
    <AuthShell breadcrumb={`Registrarse — paso ${currentStep}`}>
      <div className="flex flex-col items-center gap-6 w-full py-8">
        <Logo iconSize={28} />
        <StepIndicator current={currentStep} />
        {STEP_COMPONENTS[currentStep - 1]}
        <p className="text-center text-xs text-text-muted">
          ¿Ya tienes cuenta?{" "}
          <Link to="/login" className="text-gold underline">
            Inicia sesión
          </Link>
        </p>
      </div>
    </AuthShell>
  );
}
