import { Badge } from "../../../components/ui/Badge";
import type { OrderStatus } from "../data/mockOrders";

const STATUS_MAP: Record<OrderStatus, { variant: Parameters<typeof Badge>[0]["variant"]; label: string }> = {
  PENDING_QUEUE: { variant: "signed",  label: "En Cola" },
  SENT:          { variant: "sent",    label: "Enviada" },
  EXECUTED:      { variant: "success", label: "Ejecutada" },
  REJECTED:      { variant: "danger",  label: "Rechazada" },
  FAILED:        { variant: "danger",  label: "Fallida" },
  CANCELLED:     { variant: "neutral", label: "Cancelada" },
};

export function OrderStatusBadge({ status }: { status: string }) {
  const entry = STATUS_MAP[status as OrderStatus];
  return <Badge variant={entry?.variant ?? "neutral"}>{entry?.label ?? status}</Badge>;
}
