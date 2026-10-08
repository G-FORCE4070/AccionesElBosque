import {
  ComposedChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Cell,
  ResponsiveContainer,
} from "recharts";
import type { Candle } from "../data/mockMarket";

interface ProcessedCandle extends Candle {
  isUp: boolean;
  offset: number;
  range: number;
}

function processCandles(candles: Candle[]): ProcessedCandle[] {
  return candles.map((c) => ({
    ...c,
    isUp: c.close >= c.open,
    offset: c.low,
    range: Math.max(0.01, c.high - c.low),
  }));
}

function CandleShape(props: {
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  payload?: ProcessedCandle;
}) {
  const { x, y, width, height, payload } = props;
  if (!payload || !x || !y || !width || !height || height <= 0) return null;

  const { open, close, high, low, isUp } = payload;
  const color = isUp ? "#10B981" : "#EF4444";
  const priceRange = high - low;
  if (priceRange <= 0) return null;

  const scale = height / priceRange;
  const bodyTop = y + (high - Math.max(open, close)) * scale;
  const bodyH = Math.max(2, Math.abs(open - close) * scale);
  const cx = x + width / 2;
  const bodyX = x + width * 0.1;
  const bodyW = Math.max(2, width * 0.8);

  return (
    <g>
      {/* Full wick */}
      <line
        x1={cx} y1={y}
        x2={cx} y2={y + height}
        stroke={color} strokeWidth={1.5}
      />
      {/* Body */}
      <rect
        x={bodyX}
        y={bodyTop}
        width={bodyW}
        height={bodyH}
        fill={isUp ? color : "transparent"}
        stroke={color}
        strokeWidth={1.5}
        rx={1}
      />
    </g>
  );
}

function CandleTooltip({ active, payload }: { active?: boolean; payload?: { payload: ProcessedCandle }[] }) {
  if (!active || !payload?.[0]) return null;
  const d = payload[0].payload;
  const color = d.isUp ? "#10B981" : "#EF4444";
  return (
    <div className="bg-bg-elevated border border-border-subtle rounded-xl p-3 text-xs space-y-1 shadow-lg min-w-[110px]">
      <p className="text-text-muted mb-1 font-medium">{d.time}</p>
      <p style={{ color }}>A: <span className="tabular-nums float-right ml-4">${d.open.toFixed(2)}</span></p>
      <p style={{ color }}>H: <span className="tabular-nums float-right ml-4">${d.high.toFixed(2)}</span></p>
      <p style={{ color }}>L: <span className="tabular-nums float-right ml-4">${d.low.toFixed(2)}</span></p>
      <p style={{ color }}>C: <span className="tabular-nums float-right ml-4">${d.close.toFixed(2)}</span></p>
    </div>
  );
}

interface CandlestickChartProps {
  candles: Candle[];
  yPaddingPct?: number;
  onWheel?: (e: React.WheelEvent<HTMLDivElement>) => void;
}

export function CandlestickChart({ candles, yPaddingPct = 0.08, onWheel }: CandlestickChartProps) {
  const data = processCandles(candles);
  const barSize = Math.max(4, Math.min(36, Math.round(140 / candles.length)));

  const allValues = candles.flatMap((c) => [c.high, c.low]);
  const minVal = Math.min(...allValues);
  const maxVal = Math.max(...allValues);
  const center = (minVal + maxVal) / 2;
  // Enforce at least $25 on each side → minimum $50 visible range
  const halfSpan = Math.max((maxVal - minVal) / 2, 25);
  const extra = halfSpan * yPaddingPct;
  const domain: [number, number] = [Math.max(0, center - halfSpan - extra), center + halfSpan + extra];

  const tickInterval = Math.max(0, Math.floor(candles.length / 8) - 1);

  return (
    <div className="w-full h-full" onWheel={onWheel}>
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={data} margin={{ top: 8, right: 4, bottom: 8, left: 0 }}>
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
            width={56}
          />
          <Tooltip
            content={<CandleTooltip />}
            cursor={{ fill: "rgba(255,255,255,0.03)" }}
          />

          {/* Transparent offset — lifts the range bar to the low price */}
          <Bar
            dataKey="offset"
            stackId="c"
            fill="transparent"
            stroke="none"
            barSize={barSize}
            isAnimationActive={false}
          />

          {/* Candlestick — custom shape draws body + wicks */}
          <Bar
            dataKey="range"
            stackId="c"
            barSize={barSize}
            isAnimationActive={false}
            shape={<CandleShape />}
          >
            {data.map((d, i) => (
              <Cell key={i} fill={d.isUp ? "#10B981" : "#EF4444"} />
            ))}
          </Bar>
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}
