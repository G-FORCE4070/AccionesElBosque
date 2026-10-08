import { create } from "zustand";
import { getPortfolio } from "../services/tradingService";

interface TradingState {
  balance: number | null;
  loadPortfolio: (token: string) => Promise<void>;
  updateBalance: (n: number) => void;
}

export const useTradingStore = create<TradingState>((set) => ({
  balance: null,

  loadPortfolio: async (token) => {
    try {
      const portfolio = await getPortfolio(token);
      set({ balance: portfolio.balance });
    } catch {
      // balance stays null — shown as "–" in UI
    }
  },

  updateBalance: (n) => set({ balance: n }),
}));
