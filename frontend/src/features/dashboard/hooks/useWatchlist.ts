import { useCallback, useEffect, useState } from "react";
import {
  addSymbol,
  getWatchlist,
  removeSymbol,
} from "../services/watchlistService";
import { getStock, type StockData } from "../services/marketsService";
import { useSessionStore } from "../../../store/sessionStore";

interface UseWatchlistResult {
  symbols: string[];
  prices: Record<string, StockData>;
  loading: boolean;
  add: (symbol: string) => Promise<void>;
  remove: (symbol: string) => Promise<void>;
}

export function useWatchlist(isPremium: boolean): UseWatchlistResult {
  const token = useSessionStore((s) => s.token ?? "");
  const [symbols, setSymbols] = useState<string[]>([]);
  const [prices, setPrices] = useState<Record<string, StockData>>({});
  const [loading, setLoading] = useState(false);

  const fetchPrices = useCallback(
    (syms: string[]) => {
      if (!syms.length || !token) return;
      Promise.allSettled(syms.map((s) => getStock(s, token))).then((results) => {
        const map: Record<string, StockData> = {};
        results.forEach((r, i) => {
          if (r.status === "fulfilled") map[syms[i]] = r.value;
        });
        setPrices(map);
      });
    },
    [token]
  );

  useEffect(() => {
    if (!isPremium || !token) return;
    setLoading(true);
    getWatchlist(token)
      .then((syms) => {
        setSymbols(syms);
        fetchPrices(syms);
      })
      .catch(() => {
        setSymbols([]);
      })
      .finally(() => setLoading(false));
  }, [isPremium, token, fetchPrices]);

  const add = useCallback(
    async (symbol: string) => {
      setSymbols((prev) => [...prev, symbol]);
      try {
        await addSymbol(symbol, token);
        fetchPrices([symbol]);
      } catch {
        setSymbols((prev) => prev.filter((s) => s !== symbol));
      }
    },
    [token, fetchPrices]
  );

  const remove = useCallback(
    async (symbol: string) => {
      setSymbols((prev) => prev.filter((s) => s !== symbol));
      setPrices((prev) => {
        const next = { ...prev };
        delete next[symbol];
        return next;
      });
      try {
        await removeSymbol(symbol, token);
      } catch {
        setSymbols((prev) => [...prev, symbol]);
      }
    },
    [token]
  );

  return { symbols, prices, loading, add, remove };
}
