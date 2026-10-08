import { type HTMLAttributes } from "react";
import { cn } from "../../lib/cn";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  interactive?: boolean;
}

export function Card({ interactive = false, children, className, ...props }: CardProps) {
  return (
    <div
      className={cn(
        "bg-bg-secondary border border-border-subtle rounded-2xl p-6",
        interactive && "hover:border-gold/40 transition-colors duration-200 cursor-pointer",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}
