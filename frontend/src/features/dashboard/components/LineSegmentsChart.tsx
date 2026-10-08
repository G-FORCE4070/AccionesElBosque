import { useRef, useLayoutEffect, useState } from "react";
import {
  ComposedChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import type { Candle } from "../data/mockMarket";

const MARGIN = { top: 8, right: 4, bottom: 8, left: 0 };
const Y_AXIS_W = 56;
const X_AXIS_H = 24;

function LineTooltip({ active, payload }: { active?: boolean; payload?: { payload: Candle }[] }) {
  if (!active || !payload?.[0]) return null;
  const d = payload[0].payload;
  const isUp = d.close >= d.open;
  const color = isUp ? "#10B981" : "#EF4444";
  return (
    <div className="bg-bg-elevated border border-border-subtle rounded-xl p-3 text-xs space-y-1 shadow-lg min-w-[110px]">
      <p className="text-text-muted mb-1 font-medium">{d.time}</p>
      <p style={{ color }}>Cierre: <span className="tabular-nums float-right ml-4">${d.close.toFixed(2)}</span></p>
      <p className="text-text-muted">Apertura: <span className="tabular-nums float-right ml-4">${d.open.toFixed(2)}</span></p>
    </div>
  );
}

interface LineSegmentsChartProps {
  candles: Candle[];
  yPaddingPct?: number;
  onWheel?: (e: React.WheelEvent<HTMLDivElement>) => void;
}

export function LineSegmentsChart({ candles, yPaddingPct = 0.08, onWheel }: LineSegmentsChartProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [dims, setDims] = useState({ width: 0, height: 0 });

  useLayoutEffect(() => {
    if (!containerRef.current) return;
    const ro = new ResizeObserver((entries) => {
      const { width, height } = entries[0].contentRect;
      setDims({ width, height });
    });
    ro.observe(containerRef.current);
    return () => ro.disconnect();
  }, []);

  const closes = candles.map((c) => c.close);
  const minClose = Math.min(...closes);
  const maxClose = Math.max(...closes);
  const center = (minClose + maxClose) / 2;
  const halfSpan = Math.max((maxClose - minClose) / 2, 25);
  const extra = halfSpan * yPaddingPct;
  const yMin = Math.max(0, center - halfSpan - extra);
  const yMax = center + halfSpan + extra;
  const domain: [number, number] = [yMin, yMax];

  const chartLeft = MARGIN.left;
  const chartTop = MARGIN.top;
  const chartW = dims.width - Y_AXIS_W - MARGIN.right - chartLeft;
  const chartH = dims.height - X_AXIS_H - MARGIN.top - MARGIN.bottom;
  const N = candles.length;
  const bandW = chartW / N;

  const toX = (i: number) => chartLeft + (i + 0.5) * bandW;
  const toY = (price: number) => chartTop + ((yMax - price) / (yMax - yMin)) * chartH;

  const points = candles.map((c, i) => ({ x: toX(i), y: toY(c.close) }));
  const bottom = chartTop + chartH;

  const linePath = points.map((p, i) => `${i === 0 ? "M" : "L"} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(" ");
  const areaPath =
    points.length > 1
      ? `${linePath} L ${points[points.length - 1].x.toFixed(1)} ${bottom.toFixed(1)} L ${points[0].x.toFixed(1)} ${bottom.toFixed(1)} Z`
      : "";

  const tickInterval = Math.max(0, Math.floor(N / 8) - 1);

  return (
    <div ref={containerRef} className="relative w-full h-full" onWheel={onWheel}>
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={candles} margin={MARGIN}>
          <CartesianGrid stroke="#1F2937" strokeOpacity={0.6} vertical={false} />
          <XAxis
            dataKey="time"
            tick={{ fill: "#6B7280", fontSize: 10 }}
            axisLine={false}
            tickLine={false}
            interval={tickInterval}
          />
          <YAxis
            domain={domain}
            orientation="right"
            tick={{ fill: "#6B7280", fontSize: 10 }}
            axisLine={false}
            tickLine={false}
            tickFormatter={(v: number) => `$${v.toFixed(0)}`}
            width={Y_AXIS_W}
          />
          <Tooltip
            content={<LineTooltip />}
            cursor={{ stroke: "#374151", strokeWidth: 1, strokeDasharray: "4 4" }}
          />
          {/* invisible line — keeps tooltip + axes working */}
          <Line dataKey="close" stroke="rgba(0,0,0,0)" dot={false} isAnimationActive={false} />
        </ComposedChart>
      </ResponsiveContainer>

      {dims.width > 0 && dims.height > 0 && points.length >= 2 && (
        <svg className="absolute inset-0 pointer-events-none" width={dims.width} height={dims.height}>
          <defs>
            <linearGradient id="lgFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#D4A574" stopOpacity={0.18} />
              <stop offset="100%" stopColor="#D4A574" stopOpacity={0} />
            </linearGradient>
          </defs>
          <path d={areaPath} fill="url(#lgFill)" />
          {points.slice(1).map((pt, i) => {
            const isUp = candles[i + 1].close >= candles[i].close;
            return (
              <line
                key={i}
                x1={points[i].x.toFixed(2)}
                y1={points[i].y.toFixed(2)}
                x2={pt.x.toFixed(2)}
                y2={pt.y.toFixed(2)}
                stroke={isUp ? "#10B981" : "#EF4444"}
                strokeWidth={2.5}
                strokeLinecap="butt"
              />
            );
          })}
          {/* Junction dots to seal color transitions */}
          {points.slice(1, -1).map((pt, i) => {
            const isUp = candles[i + 1].close >= candles[i].close;
            return (
              <circle
                key={i}
                cx={pt.x.toFixed(2)}
                cy={pt.y.toFixed(2)}
                r={1.25}
                fill={isUp ? "#10B981" : "#EF4444"}
              />
            );
          })}
        </svg>
      )}
    </div>
  );
}
