const BASE_URL = "http://localhost:8083/api/v1/markets";

export interface StockData {
  symbol: string;
  companyName: string;
  market: string;
  sector: string;
  price: number | null;
  change24h: number | null;
}

export async function getStocks(
  token: string,
  markets?: string,
  sectors?: string
): Promise<StockData[]> {
  const params = new URLSearchParams();
  if (markets) params.set("markets", markets);
  if (sectors) params.set("sectors", sectors);

  const url = `${BASE_URL}/stocks${params.size > 0 ? "?" + params.toString() : ""}`;
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(`markets-service error: ${res.status}`);
  return res.json();
}

export async function getStock(symbol: string, token: string): Promise<StockData> {
  const res = await fetch(`${BASE_URL}/stocks/${symbol}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(`markets-service error: ${res.status}`);
  return res.json();
}
