import { DeliveredAlert } from "../domain/notification";
import { OrderEventKind } from "../domain/order";
import { MqpubClient } from "../messaging/mqpubClient";
import { decodePayload } from "../messaging/payloadCodec";
import { shouldResend } from "../service/resendPolicy";
import { CustomerRepository } from "../repository/customerRepository";
import { DeliveryLogRepository } from "../repository/deliveryLogRepository";
import { SmsGateway } from "./smsGateway";
import { renderAlert } from "./templates";

export interface DrainResult {
  read: number;
  delivered: DeliveredAlert[];
  failed: { orderNo: string; kind: OrderEventKind; reason: string }[];
}

/**
 * 알림 토픽 소비자. 마지막으로 읽은 위치 다음부터 읽어 문자를 보낸다.
 * 같은 키가 다시 전달되면(재처리·재기동) 두 번 보내지 않는다.
 */
export class AlertConsumer {
  private nextOffset = 0;
  private readonly handledKeys = new Set<string>();

  constructor(
    private readonly client: MqpubClient,
    private readonly topic: string,
    private readonly customers: CustomerRepository,
    private readonly gateway: SmsGateway,
    private readonly log: DeliveryLogRepository,
    private readonly senderId: string,
    private readonly limit: number,
  ) {}

  position(): number {
    return this.nextOffset;
  }

  drain(runId: number): DrainResult {
    const rows = this.client.consume(this.topic, this.nextOffset).slice(0, this.limit);
    const result: DrainResult = { read: rows.length, delivered: [], failed: [] };
    for (const row of rows) {
      this.nextOffset = row.offset + 1;
      if (this.handledKeys.has(row.key)) continue;
      this.handledKeys.add(row.key);

      const n = decodePayload(row.payload);
      if (!n) {
        result.failed.push({ orderNo: "?", kind: "PAID", reason: "bad-payload" });
        continue;
      }
      const customer = this.customers.find(n.customerId);
      if (!customer) {
        result.failed.push({ orderNo: n.orderNo, kind: n.kind, reason: "unknown-customer" });
        continue;
      }
      const receipt = this.gateway.send(this.senderId, customer.phone, renderAlert(n.kind, customer.name, n.orderNo));
      if (!receipt.ok) {
        const reason = receipt.reason ?? "sms-failed";
        result.failed.push({ orderNo: n.orderNo, kind: n.kind, reason });
        if (shouldResend(reason)) this.handledKeys.delete(row.key); // 다음 회차에 같은 키로 다시 온다
        continue;
      }
      const d: DeliveredAlert = {
        runId,
        orderNo: n.orderNo,
        customerId: n.customerId,
        kind: n.kind,
        phone: customer.phone,
        receiptId: receipt.receiptId ?? "",
      };
      this.log.append(d);
      result.delivered.push(d);
    }
    return result;
  }
}
