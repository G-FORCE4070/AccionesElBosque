import { create } from "zustand";

interface SelectedSymbolState {
  symbol: string;
  setSymbol: (symbol: string) => void;
}

export const useSelectedSymbol = create<SelectedSymbolState>((set) => ({
  symbol: "TSLA",
  setSymbol: (symbol) => set({ symbol }),
}));
