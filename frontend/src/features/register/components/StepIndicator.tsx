import { cn } from "../../../lib/cn";

const STEPS = ["Acceso", "Identidad", "Financiero", "Cumplimiento", "Preferencias", "Plan"];

interface StepIndicatorProps {
  current: number;
}

export function StepIndicator({ current }: StepIndicatorProps) {
  return (
    <div className="flex flex-wrap justify-center gap-2">
      {STEPS.map((label, i) => {
        const step = i + 1;
        const isActive = step === current;
        const isComplete = step < current;
        const isPending = step > current;

        return (
          <div
            key={step}
            className={cn(
              "flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium",
              isActive && "bg-gold-subtle text-gold",
              isComplete && "bg-gold-subtle/40 text-gold-light",
              isPending && "bg-bg-tertiary text-text-muted"
            )}
          >
            <div
              className={cn(
                "w-[18px] h-[18px] rounded-full flex items-center justify-center text-[10px] font-bold shrink-0",
                (isActive || isComplete) && "bg-gold text-bg-primary",
                isPending && "border border-border-default text-text-muted"
              )}
            >
              {isComplete ? "✓" : step}
            </div>
            {label}
          </div>
        );
      })}
    </div>
  );
}
