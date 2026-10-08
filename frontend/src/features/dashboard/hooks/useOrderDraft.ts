import { useState, useMemo, useEffect } from "react";

export type OrderSide = "buy" | "sell";

export interface OrderDraft {
  side: OrderSide;
  symbol: string;
  quantity: number;
}

export interface OrderComputed {
  subtotal: number;
  commission: number;
  total: number;
  price: number;
}

export function useOrderDraft(symbol: string, currentPrice: number) {
  const [side, setSide] = useState<OrderSide>("buy");
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    setQuantity(1);
  }, [symbol]);

  const draft: OrderDraft = { side, symbol, quantity };

  function set<K extends keyof OrderDraft>(k: K, v: OrderDraft[K]) {
    if (k === "side") setSide(v as OrderSide);
    else if (k === "quantity") setQuantity(Math.max(1, Number(v)));
  }

  const computed = useMemo<OrderComputed>(() => {
    const subtotal = quantity * currentPrice;
    const commission = subtotal * 0.02;
    const total = side === "buy" ? subtotal + commission : subtotal - commission;
    return { subtotal, commission, total, price: currentPrice };
  }, [currentPrice, quantity, side]);

  const isValid = quantity > 0;

  return { draft, set, computed, isValid };
}
