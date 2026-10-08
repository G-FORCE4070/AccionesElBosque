const BASE_URL = "http://localhost:8081/api/v1/watchlist";

export interface WatchlistApiResponse {
  symbols: string[];
}

export async function getWatchlist(token: string): Promise<string[]> {
  const res = await fetch(BASE_URL, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(`watchlist error: ${res.status}`);
  const data: WatchlistApiResponse = await res.json();
  return data.symbols;
}

export async function addSymbol(symbol: string, token: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/${symbol}`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error((body as { error?: string }).error ?? `Error ${res.status}`);
  }
}

export async function removeSymbol(symbol: string, token: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/${symbol}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(`watchlist remove error: ${res.status}`);
}
