import { Notification } from "../domain/notification";
import { CustomerRepository } from "../repository/customerRepository";
import { PendingEvent } from "../repository/orderRepository";

export interface Plan {
  notifications: Notification[];
  skippedOptOut: number;
}

/**
 * 회차에 실린 주문 이벤트를 고객 알림으로 바꾼다.
 *  - 수신 거부 고객은 제외한다.
 *  - 같은 주문·같은 이벤트가 회차 안에 여러 번 기록됐으면(운영자 재입력 등) 한 번만 알린다.
 *  - 이벤트 기록 순서를 유지한다.
 */
export function planNotifications(events: PendingEvent[], customers: CustomerRepository): Plan {
  const seen = new Set<string>();
  const notifications: Notification[] = [];
  let skippedOptOut = 0;
  for (const e of [...events].sort((a, b) => a.seq - b.seq)) {
    const customer = customers.find(e.customerId);
    if (!customer || !customer.smsOptIn) {
      skippedOptOut++;
      continue;
    }
    const id = `${e.orderNo}#${e.kind}`;
    if (seen.has(id)) continue;
    seen.add(id);
    notifications.push({ orderNo: e.orderNo, customerId: e.customerId, kind: e.kind });
  }
  return { notifications, skippedOptOut };
}
