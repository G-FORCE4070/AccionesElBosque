export type OrderStatus =
  | "PENDING_QUEUE"
  | "SENT"
  | "EXECUTED"
  | "REJECTED"
  | "FAILED"
  | "CANCELLED";

export type OrderSide = "buy" | "sell";

export interface Order {
  orderId: string;
  symbol: string;
  side: OrderSide;
  orderType: string;
  quantity: number;
  executedPrice: number | null;
  commission: number | null;
  status: OrderStatus;
  createdAt: string;
  executedAt: string | null;
}
