import { forwardRef, type InputHTMLAttributes, type ReactNode } from "react";
import { cn } from "../../lib/cn";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, leftIcon, rightIcon, error, className, ...props }, ref) => {
    return (
      <div className="flex flex-col w-full">
        {label && (
          <label className="text-xs uppercase tracking-wide text-text-muted font-medium mb-2">
            {label}
          </label>
        )}
        <div className="relative flex items-center">
          {leftIcon && (
            <span className="absolute left-4 text-text-subtle pointer-events-none">
              {leftIcon}
            </span>
          )}
          <input
            ref={ref}
            className={cn(
              "w-full h-11 px-4 bg-bg-tertiary border border-border-default text-text-primary placeholder:text-text-subtle rounded-xl",
              "focus:outline-none focus:border-border-focus transition-colors duration-150",
              leftIcon && "pl-10",
              rightIcon && "pr-10",
              error && "border-danger",
              className
            )}
            {...props}
          />
          {rightIcon && (
            <span className="absolute right-4 text-text-subtle">
              {rightIcon}
            </span>
          )}
        </div>
        {error && (
          <span className="mt-1 text-xs text-danger">{error}</span>
        )}
      </div>
    );
  }
);

Input.displayName = "Input";
