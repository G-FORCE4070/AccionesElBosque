import { useRef, type KeyboardEvent, type ClipboardEvent } from "react";
import { cn } from "../../../lib/cn";

interface OtpInputProps {
  value: string[];
  onChange: (value: string[]) => void;
  onComplete?: (code: string) => void;
  error?: boolean;
}

export function OtpInput({ value, onChange, onComplete, error }: OtpInputProps) {
  const refs = useRef<(HTMLInputElement | null)[]>([]);

  const handleChange = (index: number, raw: string) => {
    const digit = raw.replace(/\D/g, "").slice(-1);
    const next = [...value];
    next[index] = digit;
    onChange(next);

    if (digit && index < 5) {
      refs.current[index + 1]?.focus();
    }

    if (next.every(v => v !== "")) {
      onComplete?.(next.join(""));
    }
  };

  const handleKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Backspace") {
      if (value[index] !== "") {
        const next = [...value];
        next[index] = "";
        onChange(next);
      } else if (index > 0) {
        const next = [...value];
        next[index - 1] = "";
        onChange(next);
        refs.current[index - 1]?.focus();
      }
    } else if (e.key === "ArrowLeft" && index > 0) {
      refs.current[index - 1]?.focus();
    } else if (e.key === "ArrowRight" && index < 5) {
      refs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (e: ClipboardEvent) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
    if (pasted.length === 6) {
      const next = pasted.split("");
      onChange(next);
      refs.current[5]?.focus();
      onComplete?.(pasted);
    }
  };

  return (
    <div className="flex gap-2 justify-center">
      {value.map((digit, index) => (
        <input
          key={index}
          ref={el => { refs.current[index] = el; }}
          type="text"
          inputMode="numeric"
          maxLength={1}
          value={digit}
          onChange={e => handleChange(index, e.target.value)}
          onKeyDown={e => handleKeyDown(index, e)}
          onPaste={handlePaste}
          className={cn(
            "w-12 h-14 text-center text-2xl font-semibold tabular-nums",
            "bg-bg-tertiary border rounded-xl text-text-primary",
            "focus:outline-none transition-all duration-150",
            error
              ? "border-danger focus:border-danger"
              : "border-border-default focus:border-gold focus:shadow-[0_0_0_3px_rgba(212,165,116,0.15)]"
          )}
        />
      ))}
    </div>
  );
}
