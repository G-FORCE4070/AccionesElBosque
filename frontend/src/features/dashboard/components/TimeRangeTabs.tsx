import { cn } from "../../../lib/cn";

type Range = "1D" | "1S" | "1M";

interface TimeRangeTabsProps {
  value: Range;
  onChange: (range: Range) => void;
}

const RANGES: Range[] = ["1D", "1S", "1M"];

export function TimeRangeTabs({ value, onChange }: TimeRangeTabsProps) {
  return (
    <div className="flex items-center gap-1">
      {RANGES.map((r) => (
        <button
          key={r}
          type="button"
          onClick={() => onChange(r)}
          className={cn(
            "text-xs rounded-full px-3 py-1 transition-colors duration-150",
            r === value
              ? "bg-gold-subtle border border-gold text-gold"
              : "bg-transparent border border-border-default text-text-muted hover:border-border-focus hover:text-text-secondary"
          )}
        >
          {r}
        </button>
      ))}
    </div>
  );
}
