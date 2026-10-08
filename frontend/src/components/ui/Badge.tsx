import { type HTMLAttributes } from "react";
import { cn } from "../../lib/cn";

type BadgeVariant =
  | "pending"
  | "sent"
  | "signed"
  | "executing"
  | "success"
  | "danger"
  | "neutral"
  | "premium";

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
}

const variantClasses: Record<BadgeVariant, string> = {
  pending:   "bg-warning/15 text-warning",
  sent:      "bg-info/15 text-info",
  signed:    "bg-gold-subtle text-gold",
  executing: "bg-purple-500/15 text-purple-400",
  success:   "bg-success-bg/40 text-success",
  danger:    "bg-danger-bg/40 text-danger",
  neutral:   "bg-bg-tertiary text-text-muted",
  premium:   "bg-gold-subtle text-gold",
};

export function Badge({
  variant = "neutral",
  children,
  className,
  ...props
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center px-3 py-1 rounded-full text-xs font-medium",
        variantClasses[variant],
        className
      )}
      {...props}
    >
      {children}
    </span>
  );
}
