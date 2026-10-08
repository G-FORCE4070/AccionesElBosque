export interface Transaction {
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
