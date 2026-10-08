import { cn } from "../../lib/cn";

interface Tab {
  key: string;
  label: string;
  badge?: number;
}

interface TabsProps {
  tabs: Tab[];
  active: string;
  onChange: (key: string) => void;
  className?: string;
}

export function Tabs({ tabs, active, onChange, className }: TabsProps) {
  return (
    <div className={cn("flex gap-2", className)}>
      {tabs.map((tab) => {
        const isActive = tab.key === active;
        return (
          <button
            key={tab.key}
            type="button"
            onClick={() => onChange(tab.key)}
            className={cn(
              "inline-flex items-center gap-2 px-5 py-2 rounded-full text-sm font-medium transition-colors duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-gold/50",
              isActive
                ? "bg-gold-subtle text-gold border border-gold/30"
                : "bg-bg-tertiary text-text-muted hover:text-text-secondary"
            )}
          >
            {tab.label}
            {tab.badge !== undefined && (
              <span
                className={cn(
                  "inline-flex items-center justify-center w-5 h-5 rounded-full text-xs",
                  isActive ? "bg-gold text-bg-primary" : "bg-bg-elevated text-text-muted"
                )}
              >
                {tab.badge}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
