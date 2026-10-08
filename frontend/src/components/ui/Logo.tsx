import { TrendingUp } from "lucide-react";
import { cn } from "../../lib/cn";

interface LogoProps {
  className?: string;
  iconSize?: number;
}

export function Logo({ className, iconSize = 24 }: LogoProps) {
  return (
    <div className={cn("flex items-center gap-2", className)}>
      <TrendingUp size={iconSize} className="text-gold" strokeWidth={2} />
      <div className="flex flex-col leading-tight">
        <span className="text-text-primary font-semibold text-sm">Acciones</span>
        <span className="text-text-muted text-xs">ElBosque</span>
      </div>
    </div>
  );
}
