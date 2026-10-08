import type { ReactNode } from "react";

interface WizardCardProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
}

export function WizardCard({ title, subtitle, children }: WizardCardProps) {
  return (
    <div className="bg-bg-secondary rounded-2xl p-8 border border-gold/15 w-[480px] max-w-full">
      <h2 className="font-display text-3xl text-text-primary">{title}</h2>
      {subtitle && (
        <p className="mt-1 text-sm text-text-muted">{subtitle}</p>
      )}
      <div className="mt-6">{children}</div>
    </div>
  );
}
