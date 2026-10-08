import { MarketStatusBadge } from "../components/MarketStatusBadge";
import { BalancePill } from "../components/BalancePill";
import { WatchlistPanel } from "../components/WatchlistPanel";
import { ChartPanel } from "../components/ChartPanel";
import { NewOrderPanel } from "../components/NewOrderPanel";

export function DashboardPage() {
  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-display text-4xl text-text-primary font-bold">
          Terminal de Trading
        </h1>
        <div className="flex items-center gap-3">
          <MarketStatusBadge />
          <BalancePill />
        </div>
      </div>

      {/* Three-panel layout */}
      <div className="grid grid-cols-[280px_1fr_360px] gap-4 flex-1 min-h-0">
        <WatchlistPanel />
        <ChartPanel />
        <NewOrderPanel />
      </div>
    </div>
  );
}
