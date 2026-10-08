import { Check } from "lucide-react";
import { cn } from "../../lib/cn";

interface CheckboxProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  disabled?: boolean;
  className?: string;
}

export function Checkbox({ checked, onChange, label, disabled = false, className }: CheckboxProps) {
  return (
    <label className={cn("inline-flex items-center gap-2 cursor-pointer", disabled && "opacity-50 cursor-not-allowed", className)}>
      <button
        type="button"
        role="checkbox"
        aria-checked={checked}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={cn(
          "w-5 h-5 rounded flex items-center justify-center border transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-gold/50",
          checked
            ? "bg-gold border-gold"
            : "bg-transparent border-border-default hover:border-gold"
        )}
      >
        {checked && <Check size={12} className="text-bg-primary" strokeWidth={3} />}
      </button>
      {label && (
        <span className="text-sm text-text-secondary select-none">{label}</span>
      )}
    </label>
  );
}
