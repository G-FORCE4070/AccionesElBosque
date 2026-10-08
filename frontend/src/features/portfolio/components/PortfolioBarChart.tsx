import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, Cell, ResponsiveContainer,
} from "recharts";
import type { Holding } from "../data/mockHoldings";

const fmt = new Intl.NumberFormat("en-US", { minimumFractionDigits: 0, maximumFractionDigits: 0 });

function BarTooltip({ active, payload }: { active?: boolean; payload?: { payload: Holding }[] }) {
  if (!active || !payload?.[0]) return null;
  const d = payload[0].payload;
  const isPos = d.gainDollars == null || d.gainDollars >= 0;
  return (
    <div className="bg-bg-elevated border border-border-subtle rounded-xl p-3 text-xs space-y-1 shadow-lg">
      <p className="font-bold text-text-primary mb-1">{d.symbol}</p>
      <p className="text-text-muted">
        Valor total:{" "}
        <span className="tabular-nums text-text-secondary">
          {d.totalValue != null ? `$${fmt.format(d.totalValue)}` : "—"}
        </span>
      </p>
      <p className={d.gainDollars == null ? "text-text-muted" : isPos ? "text-success" : "text-danger"}>
        {d.gainDollars != null
          ? `G/P: ${isPos ? "+" : ""}$${d.gainDollars.toFixed(2)} (${isPos ? "+" : ""}${d.gainPct?.toFixed(2) ?? "—"}%)`
          : "G/P: —"}
      </p>
    </div>
  );
}

interface PortfolioBarChartProps {
  holdings: Holding[];
}

export function PortfolioBarChart({ holdings }: PortfolioBarChartProps) {
  return (
    <div style={{ height: Math.max(280, holdings.length * 56) }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={holdings}
          layout="vertical"
          margin={{ top: 4, right: 80, bottom: 4, left: 8 }}
          barSize={22}
        >
          <CartesianGrid stroke="#1F2937" strokeOpacity={0.5} horizontal={false} />
          <XAxis
            type="number"
            tick={{ fill: "#6B7280", fontSize: 10 }}
            axisLine={false}
            tickLine={false}
            tickFormatter={(v) => `$${fmt.format(v)}`}
          />
          <YAxis
            type="category"
            dataKey="symbol"
            tick={{ fill: "#E5E7EB", fontSize: 12, fontWeight: 600 }}
            axisLine={false}
            tickLine={false}
            width={52}
          />
          <Tooltip content={<BarTooltip />} cursor={{ fill: "rgba(255,255,255,0.03)" }} />
          <Bar dataKey="totalValue" radius={[0, 4, 4, 0]} isAnimationActive={true}>
            {holdings.map((h, i) => (
              <Cell
                key={i}
                fill={h.gainDollars == null ? "#6B7280" : h.gainDollars >= 0 ? "#10B981" : "#EF4444"}
                fillOpacity={0.85}
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
