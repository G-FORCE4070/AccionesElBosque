interface SelectRowProps {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: { value: string; label: string }[];
  disabled?: boolean;
}

const selectClass =
  "bg-bg-tertiary border border-border-default rounded-lg px-3 py-1.5 text-sm text-text-secondary focus:outline-none focus:border-border-focus transition-colors duration-150 w-[180px] disabled:opacity-50 disabled:cursor-not-allowed";

export function SelectRow({ label, value, onChange, options, disabled }: SelectRowProps) {
  return (
    <div className="flex items-center justify-between py-4 border-b border-border-subtle/30 last:border-0">
      <span className={`text-sm text-text-secondary${disabled ? " opacity-50" : ""}`}>{label}</span>
      <select
        className={selectClass}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
    </div>
  );
}
