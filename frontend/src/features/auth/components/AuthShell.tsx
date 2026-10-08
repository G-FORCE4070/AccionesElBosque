import type { ReactNode } from "react";

interface AuthShellProps {
  breadcrumb: string;
  children: ReactNode;
}

export function AuthShell({ breadcrumb, children }: AuthShellProps) {
  return (
    <div className="min-h-screen bg-bg-primary bg-grid-subtle bg-grid relative">
      <div className="absolute top-4 left-6 text-xs text-text-subtle">
        {breadcrumb}
      </div>
      <div className="min-h-screen flex items-center justify-center px-6">
        {children}
      </div>
    </div>
  );
}
