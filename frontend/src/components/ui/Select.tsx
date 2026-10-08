import { useState, useRef, useEffect } from "react";
import { ChevronDown } from "lucide-react";
import { cn } from "../../lib/cn";

interface SelectOption {
  value: string;
  label: string;
}

interface SelectProps {
  options: SelectOption[];
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  label?: string;
  disabled?: boolean;
  className?: string;
}

export function Select({
  options,
  value,
  onChange,
  placeholder = "Seleccionar...",
  label,
  disabled = false,
  className,
}: SelectProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const selected = options.find((o) => o.value === value);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div className={cn("flex flex-col w-full", className)} ref={ref}>
      {label && (
        <label className="text-xs uppercase tracking-wide text-text-muted font-medium mb-2">
          {label}
        </label>
      )}
      <div className="relative">
        <button
          type="button"
          disabled={disabled}
          onClick={() => setOpen((o) => !o)}
          className={cn(
            "w-full h-11 px-4 pr-10 bg-bg-tertiary border border-border-default text-left rounded-xl",
            "focus:outline-none focus:border-border-focus transition-colors duration-150",
            "disabled:opacity-50 disabled:cursor-not-allowed",
            selected ? "text-text-primary" : "text-text-subtle"
          )}
        >
          {selected ? selected.label : placeholder}
        </button>
        <ChevronDown
          size={16}
          className={cn(
            "absolute right-4 top-1/2 -translate-y-1/2 text-text-muted transition-transform duration-150 pointer-events-none",
            open && "rotate-180"
          )}
        />
        {open && (
          <div className="absolute z-50 w-full mt-1 bg-bg-elevated border border-border-subtle rounded-xl overflow-hidden shadow-lg">
            {options.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() => {
                  onChange?.(opt.value);
                  setOpen(false);
                }}
                className={cn(
                  "w-full px-4 py-3 text-left text-sm text-text-secondary hover:bg-bg-tertiary transition-colors duration-100",
                  opt.value === value && "text-gold bg-gold-subtle/30"
                )}
              >
                {opt.label}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
