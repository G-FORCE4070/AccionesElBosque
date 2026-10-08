import type { ReactNode } from "react";
import { Toggle } from "../../../components/ui/Toggle";

interface ToggleRowProps {
  label: ReactNode;
  subLabel?: string;
  checked: boolean;
  onChange: (v: boolean) => void;
  disabled?: boolean;
}

export function ToggleRow({ label, subLabel, checked, onChange, disabled }: ToggleRowProps) {
  return (
    <div className="flex items-center justify-between py-4 border-b border-border-subtle/30 last:border-0">
      <div className={disabled ? "opacity-50" : ""}>
        <div className="text-sm text-text-secondary flex items-center gap-2">{label}</div>
        {subLabel && <p className="text-xs text-text-muted mt-0.5">{subLabel}</p>}
      </div>
      <Toggle checked={checked} onChange={onChange} disabled={disabled} />
    </div>
  );
}
