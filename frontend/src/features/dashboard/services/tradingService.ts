const BASE_URL = "http://localhost:8082/api/v1";

export interface OrderResult {
  orderId: string;
  symbol: string;
  side: string;
  orderType: string;
  quantity: number;
  estimatedPrice: number;
  executedPrice: number;
  commission: number;
  totalAmount: number;
  newBalance: number;
  status: string;
  executedAt: string;
}

export interface QueuedOrderResult {
  orderId: string;
  symbol: string;
  side: string;
  quantity: number;
  estimatedPrice: number;
  status: "PENDING_QUEUE";
  queuedAt: string;
  message: string;
}

export interface HoldingItem {
  symbol: string;
  quantity: number;
  avgCost: number;
  currentPrice: number | null;
  totalValue: number | null;
  gainPct: number | null;
  gainDollars: number | null;
}

export interface PortfolioResult {
  balance: number;
  pricesAvailable: boolean;
  holdings: HoldingItem[];
}

export class MarketClosedError extends Error {
  constructor() {
    super("El mercado está cerrado");
  }
}

export async function placeOrder(
  side: "buy" | "sell",
  symbol: string,
  quantity: number,
  token: string,
  queue?: boolean
): Promise<OrderResult | QueuedOrderResult> {
  const res = await fetch(`${BASE_URL}/orders/${side}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ symbol, quantity, orderType: "market", ...(queue ? { queue: true } : {}) }),
  });

  if (res.status === 202) {
    return res.json() as Promise<QueuedOrderResult>;
  }

  if (res.status === 422) {
    throw new MarketClosedError();
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error ?? "Error al procesar la orden");
  }

  return res.json() as Promise<OrderResult>;
}

export interface OrderHistoryItem {
  orderId: string;
  symbol: string;
  side: string;
  orderType: string;
  quantity: number;
  executedPrice: number | null;
  commission: number | null;
  status: string;
  createdAt: string;
  executedAt: string | null;
}

export interface OrderHistoryResponse {
  content: OrderHistoryItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface TransactionItem {
  transactionId: string;
  date: string;
  type: string;
  symbol: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  commission: number;
  total: number;
}

export interface TransactionHistoryResponse {
  content: TransactionItem[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  totalCommission: number;
}

export async function getOrders(
  token: string,
  params: { symbol?: string; status?: string; startDate?: string; endDate?: string },
  page: number,
  size: number
): Promise<OrderHistoryResponse> {
  const q = new URLSearchParams();
  if (params.symbol) q.set("symbol", params.symbol);
  if (params.status) q.set("status", params.status);
  if (params.startDate) q.set("startDate", params.startDate);
  if (params.endDate) q.set("endDate", params.endDate);
  q.set("page", String(page));
  q.set("size", String(size));
  const res = await fetch(`${BASE_URL}/orders?${q}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error("Error al obtener órdenes");
  return res.json();
}

export async function getTransactions(
  token: string,
  params: { type?: string; symbol?: string; startDate?: string; endDate?: string },
  page: number,
  size: number
): Promise<TransactionHistoryResponse> {
  const q = new URLSearchParams();
  if (params.type) q.set("type", params.type);
  if (params.symbol) q.set("symbol", params.symbol);
  if (params.startDate) q.set("startDate", params.startDate);
  if (params.endDate) q.set("endDate", params.endDate);
  q.set("page", String(page));
  q.set("size", String(size));
  const res = await fetch(`${BASE_URL}/transactions?${q}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error("Error al obtener transacciones");
  return res.json();
}

export async function cancelOrder(orderId: string, token: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/orders/${orderId}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error ?? "Error al cancelar la orden");
  }
}

export async function getPortfolio(token: string): Promise<PortfolioResult> {
  const res = await fetch(`${BASE_URL}/portfolio`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error("Error al obtener portafolio");
  return res.json();
}
