const fmt = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

interface CommissionBreakdownProps {
  total: number;
  pct: number;
}

export function CommissionBreakdown({ total, pct }: CommissionBreakdownProps) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-sm font-semibold text-text-primary tabular-nums">
        ${fmt.format(total)}
      </span>
      <span className="bg-bg-tertiary border border-border-subtle rounded-full px-2 py-0.5 text-[10px] text-text-muted">
        {pct.toFixed(1)}%
      </span>
      <span className="text-xs text-text-muted">ElBosque</span>
    </div>
  );
}
