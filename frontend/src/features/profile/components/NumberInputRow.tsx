interface NumberInputRowProps {
  label: string;
  value: number;
  onChange: (v: number) => void;
  min?: number;
  max?: number;
}

const inputClass =
  "bg-bg-tertiary border border-border-default rounded-lg px-3 py-1.5 text-sm text-text-secondary focus:outline-none focus:border-border-focus transition-colors duration-150 w-[180px]";

export function NumberInputRow({ label, value, onChange, min = 1, max }: NumberInputRowProps) {
  return (
    <div className="flex items-center justify-between py-4 border-b border-border-subtle/30 last:border-0">
      <span className="text-sm text-text-secondary">{label}</span>
      <input
        type="number"
        className={inputClass}
        value={value}
        min={min}
        max={max}
        onChange={(e) => onChange(Number(e.target.value))}
      />
    </div>
  );
}
