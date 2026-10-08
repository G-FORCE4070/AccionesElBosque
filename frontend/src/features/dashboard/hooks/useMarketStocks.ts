import { useEffect, useRef, useState } from "react";
import { getStocks, type StockData } from "../services/marketsService";
import { useSessionStore } from "../../../store/sessionStore";
import { useProfileStore } from "../../profile/store/profileStore";

interface UseMarketStocksResult {
  stocks: StockData[];
  loading: boolean;
  error: boolean;
  refetch: () => void;
}

export function useMarketStocks(): UseMarketStocksResult {
  const token = useSessionStore((s) => s.token ?? "");
  const market = useProfileStore((s) => s.market);
  const sector = useProfileStore((s) => s.sector);

  const [stocks, setStocks] = useState<StockData[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const fetchRef = useRef(0);

  const fetchStocks = () => {
    const id = ++fetchRef.current;
    setLoading(true);
    setError(false);
    getStocks(token, market || undefined, sector || undefined)
      .then((data) => {
        if (id === fetchRef.current) {
          setStocks(data);
          setLoading(false);
        }
      })
      .catch(() => {
        if (id === fetchRef.current) {
          setError(true);
          setLoading(false);
        }
      });
  };

  useEffect(() => {
    fetchStocks();
    const interval = setInterval(fetchStocks, 30_000);
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, market, sector]);

  return { stocks, loading, error, refetch: fetchStocks };
}
