import { cn } from "../../../../lib/cn";

type Option = { label: string; value: string; disabled?: boolean };

type ChipGroupSingle = {
  options: Option[];
  multiple?: false;
  value: string;
  onChange: (value: string) => void;
  className?: string;
};

type ChipGroupMulti = {
  options: Option[];
  multiple: true;
  value: string[];
  onChange: (value: string[]) => void;
  className?: string;
};

type ChipGroupProps = ChipGroupSingle | ChipGroupMulti;

export function ChipGroup(props: ChipGroupProps) {
  const { options, className } = props;

  const isSelected = (v: string) =>
    props.multiple ? props.value.includes(v) : props.value === v;

  const handleClick = (v: string, disabled?: boolean) => {
    if (disabled) return;
    if (props.multiple) {
      const current = props.value;
      props.onChange(
        current.includes(v) ? current.filter((x) => x !== v) : [...current, v]
      );
    } else {
      props.onChange(v);
    }
  };

  return (
    <div className={cn("flex flex-wrap gap-2", className)}>
      {options.map(({ label, value, disabled }) => (
        <button
          key={value}
          type="button"
          onClick={() => handleClick(value, disabled)}
          disabled={disabled}
          className={cn(
            "rounded-full px-4 py-2 text-sm font-medium transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-gold/50",
            disabled
              ? "border border-border-default text-text-muted/40 bg-transparent cursor-not-allowed"
              : isSelected(value)
              ? "border border-gold bg-gold-subtle text-gold"
              : "border border-border-default text-text-muted bg-transparent hover:border-gold/40"
          )}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
