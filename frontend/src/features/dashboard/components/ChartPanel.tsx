import { useState, useEffect, useCallback } from "react";
import {
  ZoomIn, ZoomOut,
  ChevronLeft, ChevronRight,
  ArrowUpFromLine, ArrowDownFromLine,
  CandlestickChart as CandleIcon,
  TrendingUp,
} from "lucide-react";
import { Card } from "../../../components/ui/Card";
import { CandlestickChart } from "./CandlestickChart";
import { LineSegmentsChart } from "./LineSegmentsChart";
import { TimeRangeTabs } from "./TimeRangeTabs";
import { useSelectedSymbol } from "../hooks/useSelectedSymbol";
import { generateCandles, mockStocks } from "../data/mockMarket";

type Range = "1D" | "1S" | "1M";
type ChartMode = "candles" | "line";

const MIN_VISIBLE = 5;
const Y_STEP = 0.12;
const Y_MIN = 0.01;
const Y_MAX = 3.0;

function ZoomBtn({
  onClick, disabled, title, children,
}: {
  onClick: () => void; disabled?: boolean; title: string; children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      title={title}
      className="w-7 h-7 flex items-center justify-center rounded-lg border border-border-subtle text-text-muted hover:border-gold/40 hover:text-gold transition-colors duration-150 disabled:opacity-30 disabled:cursor-not-allowed"
    >
      {children}
    </button>
  );
}

export function ChartPanel() {
  const [range, setRange]           = useState<Range>("1D");
  const [mode, setMode]             = useState<ChartMode>("candles");
  const [visibleCount, setVisible]  = useState(20);
  const [panOffset, setPan]         = useState(0);
  const [yPadding, setYPad]         = useState(0.08);

  const symbol = useSelectedSymbol((s) => s.symbol);
  const stock  = mockStocks.find((s) => s.symbol === symbol) ?? mockStocks[0];
  const all    = generateCandles(symbol, range);
  const total  = all.length;

  const maxOffset      = Math.max(0, total - visibleCount);
  const safeOffset     = Math.min(panOffset, maxOffset);
  const visibleCandles = all.slice(safeOffset, safeOffset + visibleCount);

  useEffect(() => { setPan(0); setVisible(20); }, [symbol, range]);

  const handleWheel = useCallback((e: React.WheelEvent<HTMLDivElement>) => {
    e.preventDefault();
    setVisible((p) => Math.max(MIN_VISIBLE, Math.min(total, p + (e.deltaY > 0 ? 3 : -3))));
  }, [total]);

  const zoomIn  = () => setVisible((p) => Math.max(MIN_VISIBLE, p - 3));
  const zoomOut = () => setVisible((p) => Math.min(total, p + 3));
  const panLeft  = () => setPan((p) => Math.max(0, p - 3));
  const panRight = () => setPan((p) => Math.min(maxOffset, p + 3));
  const expandY   = () => setYPad((p) => Math.min(Y_MAX, +(p + Y_STEP).toFixed(2)));
  const compressY = () => setYPad((p) => Math.max(Y_MIN, +(p - Y_STEP).toFixed(2)));

  return (
    <Card className="flex flex-col gap-3 p-4 min-h-0">

      {/* Row 1: symbol + mode toggle + time tabs */}
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 min-w-0">
          <span className="text-base font-bold text-text-primary">{symbol}</span>
          <span className="text-text-subtle">·</span>
          <span className="text-sm text-text-muted truncate">{stock.name}</span>
          <span className={`text-sm tabular-nums ml-1 shrink-0 ${stock.changePct >= 0 ? "text-success" : "text-danger"}`}>
            {stock.changePct >= 0 ? "+" : ""}{stock.changePct.toFixed(1)}%
          </span>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {/* Chart mode toggle */}
          <div className="flex rounded-lg border border-border-subtle overflow-hidden">
            <button
              type="button"
              onClick={() => setMode("candles")}
              title="Velas japonesas"
              className={`w-8 h-7 flex items-center justify-center text-xs transition-colors duration-150 ${
                mode === "candles"
                  ? "bg-gold/20 text-gold"
                  : "text-text-muted hover:text-text-secondary"
              }`}
            >
              <CandleIcon size={13} />
            </button>
            <button
              type="button"
              onClick={() => setMode("line")}
              title="Gráfico de línea"
              className={`w-8 h-7 flex items-center justify-center text-xs transition-colors duration-150 ${
                mode === "line"
                  ? "bg-gold/20 text-gold"
                  : "text-text-muted hover:text-text-secondary"
              }`}
            >
              <TrendingUp size={13} />
            </button>
          </div>

          <TimeRangeTabs value={range} onChange={setRange} />
        </div>
      </div>

      {/* Row 2: zoom controls */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1">
          <span className="text-[10px] text-text-subtle uppercase tracking-wide mr-1">Tiempo</span>
          <ZoomBtn onClick={panLeft}  disabled={safeOffset === 0}          title="Desplazar izquierda"><ChevronLeft size={12} /></ZoomBtn>
          <ZoomBtn onClick={zoomIn}   disabled={visibleCount <= MIN_VISIBLE} title="Acercar"><ZoomIn size={12} /></ZoomBtn>
          <ZoomBtn onClick={zoomOut}  disabled={visibleCount >= total}       title="Alejar"><ZoomOut size={12} /></ZoomBtn>
          <ZoomBtn onClick={panRight} disabled={safeOffset >= maxOffset}    title="Desplazar derecha"><ChevronRight size={12} /></ZoomBtn>
          <span className="text-[10px] text-text-subtle ml-1 tabular-nums">{visibleCount}/{total}</span>
        </div>

        <div className="flex items-center gap-1">
          <span className="text-[10px] text-text-subtle uppercase tracking-wide mr-1">Precio</span>
          <ZoomBtn onClick={compressY} disabled={yPadding <= Y_MIN} title="Comprimir eje de precio"><ArrowDownFromLine size={12} /></ZoomBtn>
          <ZoomBtn onClick={expandY}   disabled={yPadding >= Y_MAX} title="Expandir eje de precio"><ArrowUpFromLine size={12} /></ZoomBtn>
        </div>
      </div>

      {/* Chart area */}
      <div className="flex-1 min-h-0" style={{ height: 520 }}>
        {mode === "candles" ? (
          <CandlestickChart candles={visibleCandles} yPaddingPct={yPadding} onWheel={handleWheel} />
        ) : (
          <LineSegmentsChart candles={visibleCandles} yPaddingPct={yPadding} onWheel={handleWheel} />
        )}
      </div>
    </Card>
  );
}
