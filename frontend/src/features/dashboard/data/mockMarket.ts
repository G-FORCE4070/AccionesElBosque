export interface Stock {
  symbol: string;
  name: string;
  market: string;
  price: number;
  changePct: number;
}

export interface Candle {
  time: string;
  open: number;
  high: number;
  low: number;
  close: number;
}

export const mockStocks: Stock[] = [
  { symbol: "TSLA", name: "Tesla Inc.", market: "NASDAQ", price: 245.18, changePct: -0.8 },
  { symbol: "AAPL", name: "Apple Inc.", market: "NASDAQ", price: 178.25, changePct: 1.2 },
  { symbol: "MSFT", name: "Microsoft Corp.", market: "NASDAQ", price: 332.40, changePct: 0.4 },
  { symbol: "NVDA", name: "NVIDIA Corp.", market: "NASDAQ", price: 452.18, changePct: -2.1 },
  { symbol: "GOOGL", name: "Alphabet Inc.", market: "NASDAQ", price: 140.82, changePct: 0.9 },
  { symbol: "AMZN", name: "Amazon.com Inc.", market: "NASDAQ", price: 186.55, changePct: 1.5 },
  { symbol: "META", name: "Meta Platforms", market: "NASDAQ", price: 505.32, changePct: -0.3 },
];

export const mockWatchlist = ["AAPL", "TSLA", "NVDA"];

function seededRng(seed: number) {
  let s = seed;
  return () => {
    s = (s * 1664525 + 1013904223) & 0xffffffff;
    return (s >>> 0) / 4294967295;
  };
}

export function generateCandles(symbol: string, range: "1D" | "1S" | "1M"): Candle[] {
  const stock = mockStocks.find((s) => s.symbol === symbol) ?? mockStocks[0];
  const seed = symbol.split("").reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
  const rand = seededRng(seed + (range === "1D" ? 1 : range === "1S" ? 2 : 3));

  const counts = { "1D": 30, "1S": 35, "1M": 22 };
  const count = counts[range];
  const candles: Candle[] = [];
  let price = stock.price * (0.9 + rand() * 0.05);

  for (let i = 0; i < count; i++) {
    const move = (rand() - 0.48) * price * 0.025;
    const open = price;
    const close = Math.max(1, price + move);
    const volatility = Math.abs(move) + price * 0.008 * rand();
    const high = Math.max(open, close) + volatility * rand();
    const low = Math.max(1, Math.min(open, close) - volatility * rand());

    let time: string;
    if (range === "1D") {
      const minuteOffset = i * Math.floor(390 / count);
      const h = 9 + Math.floor((minuteOffset + 30) / 60);
      const m = (minuteOffset + 30) % 60;
      time = `${h}:${String(m).padStart(2, "0")}`;
    } else if (range === "1S") {
      const days = ["Lun", "Mar", "Mié", "Jue", "Vie"];
      time = `${days[i % 5]}${i > 4 ? ` S${Math.floor(i / 5) + 1}` : ""}`;
    } else {
      const months = ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"];
      time = months[i % 12];
    }

    candles.push({ time, open, high, low, close });
    price = close;
  }

  return candles;
}

export function generateSparkline(symbol: string, points = 12): number[] {
  const stock = mockStocks.find((s) => s.symbol === symbol) ?? mockStocks[0];
  const seed = symbol.split("").reduce((acc, ch) => acc + ch.charCodeAt(0), 99);
  const rand = seededRng(seed);
  const data: number[] = [];
  let p = stock.price * 0.97;
  for (let i = 0; i < points; i++) {
    p = Math.max(1, p + (rand() - 0.48) * p * 0.02);
    data.push(p);
  }
  return data;
}
