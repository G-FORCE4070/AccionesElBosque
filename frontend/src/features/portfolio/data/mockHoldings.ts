export interface Holding {
  symbol: string;
  quantity: number;
  avgCost: number;
  currentPrice: number | null;
  totalValue: number | null;
  gainPct: number | null;
  gainDollars: number | null;
}

export const mockHoldings: Holding[] = [
  { symbol: "AAPL",  quantity: 50, avgCost: 165.30, currentPrice: 178.25, totalValue:  8912.50, gainDollars:  647.50, gainPct:  7.83 },
  { symbol: "MSFT",  quantity: 20, avgCost: 310.50, currentPrice: 332.40, totalValue:  6648.00, gainDollars:  438.00, gainPct:  7.05 },
  { symbol: "JNJ",   quantity: 30, avgCost: 160.00, currentPrice: 162.30, totalValue:  4869.00, gainDollars:   69.00, gainPct:  1.44 },
  { symbol: "NVDA",  quantity: 10, avgCost: 480.00, currentPrice: 452.18, totalValue:  4521.80, gainDollars: -278.20, gainPct: -5.80 },
  { symbol: "JPM",   quantity: 20, avgCost: 140.00, currentPrice: 145.60, totalValue:  2912.00, gainDollars:  112.00, gainPct:  4.00 },
  { symbol: "GOOGL", quantity: 15, avgCost: 135.20, currentPrice: 140.82, totalValue:  2112.30, gainDollars:   84.30, gainPct:  4.16 },
  { symbol: "TSLA",  quantity:  8, avgCost: 260.00, currentPrice: 245.18, totalValue:  1961.44, gainDollars: -118.56, gainPct: -5.70 },
];
