import { useProfileStore } from "../../profile/store/profileStore";

// NYSE and NASDAQ: 09:30–16:00 ET, Mon–Fri. Uses America/New_York to handle DST automatically.
function isMarketOpen(): boolean {
  const now = new Date();

  const etParts = new Intl.DateTimeFormat("en-US", {
    timeZone: "America/New_York",
    weekday: "short",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).formatToParts(now);

  const get = (type: string) => etParts.find((p) => p.type === type)?.value ?? "";
  const weekday = get("weekday");
  if (weekday === "Sat" || weekday === "Sun") return false;

  const totalMinutes = parseInt(get("hour")) * 60 + parseInt(get("minute"));
  return totalMinutes >= 9 * 60 + 30 && totalMinutes < 16 * 60;
}

const MARKET_LABELS: Record<string, string> = {
  nyse: "NYSE",
  nasdaq: "NASDAQ",
};

export function MarketStatusBadge() {
  const market = useProfileStore((s) => s.market);
  const open = isMarketOpen();
  const label = MARKET_LABELS[market] ?? market.toUpperCase();

  return (
    <div className="flex items-center gap-2 bg-bg-secondary border border-border-subtle rounded-full px-4 py-2">
      <span
        className={`w-2 h-2 rounded-full shrink-0 ${open ? "bg-success animate-pulse" : "bg-text-muted"}`}
      />
      <span className="text-sm text-text-secondary">
        {label} · {open ? "Abierto" : "Cerrado"}
      </span>
      <span className="w-px h-3 bg-border-default" />
      <span className="text-sm text-text-muted">
        {open ? "9:30–16:00 ET" : "Fuera de horario"}
      </span>
    </div>
  );
}
