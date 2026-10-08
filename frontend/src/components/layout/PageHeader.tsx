interface PageHeaderProps {
  readonly title: string;
  readonly subtitle?: string;
  readonly action?: React.ReactNode;
}

export function PageHeader({ title, subtitle, action }: PageHeaderProps) {
  return (
    <header className="mb-6 flex items-start justify-between gap-4">
      <div>
        <h1 className="font-display text-4xl text-text-primary font-bold">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-text-muted">{subtitle}</p>}
      </div>
      {action && <div className="shrink-0 mt-1">{action}</div>}
    </header>
  );
}
