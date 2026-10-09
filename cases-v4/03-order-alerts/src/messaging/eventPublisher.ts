import { Notification } from "../domain/notification";
import { messageKey } from "./keys";
import { MqpubClient, PublishResult, WireMessage } from "./mqpubClient";
import { encodePayload } from "./payloadCodec";

/** 발송 회차 하나의 알림을 한 배치로 발행한다(회차 단위 순서·원자성). */
export class AlertPublisher {
  constructor(
    private readonly client: MqpubClient,
    private readonly topic: string,
    private readonly keyNamespace: string,
  ) {}

  toWire(n: Notification): WireMessage {
    return { key: messageKey(this.keyNamespace, n.kind, n.orderNo), payload: encodePayload(n) };
  }

  publishRun(notifications: Notification[]): PublishResult {
    return this.client.publish(this.topic, notifications.map((n) => this.toWire(n)));
  }
}
