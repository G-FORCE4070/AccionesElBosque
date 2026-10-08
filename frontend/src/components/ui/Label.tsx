import { type LabelHTMLAttributes } from "react";
import { cn } from "../../lib/cn";

interface LabelProps extends LabelHTMLAttributes<HTMLLabelElement> {}

export function Label({ children, className, ...props }: LabelProps) {
  return (
    <label
      className={cn(
        "text-xs uppercase tracking-wide text-text-muted font-medium mb-2 block",
        className
      )}
      {...props}
    >
      {children}
    </label>
  );
}
