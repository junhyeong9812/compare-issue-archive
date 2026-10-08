import { orderTotal } from "../domain/order";
import { OrderAggregate } from "../service/orderQueryService";
import { Badge, badgeFor } from "../ui/badges";
import { formatWon } from "../util/money";

export interface OrderRow {
  id: string;
  customer: string;
  total: string;
  status: Badge;
  payment: Badge | null;
  shipment: Badge | null;
}

export function presentRow(agg: OrderAggregate, locale: string): OrderRow {
  return {
    id: agg.order.id,
    customer: agg.order.customer,
    total: formatWon(orderTotal(agg.order), locale),
    status: badgeFor(agg.order.status),
    payment: agg.payment ? badgeFor(agg.payment.status) : null,
    shipment: agg.shipment ? badgeFor(agg.shipment.status) : null,
  };
}
