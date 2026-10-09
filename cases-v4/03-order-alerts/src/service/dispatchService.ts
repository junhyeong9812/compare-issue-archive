import { DispatchRun } from "../domain/notification";
import { AlertPublisher } from "../messaging/eventPublisher";
import { AlertConsumer } from "../notifier/consumerWorker";
import { CustomerRepository } from "../repository/customerRepository";
import { DeliveryLogRepository } from "../repository/deliveryLogRepository";
import { OrderRepository } from "../repository/orderRepository";
import { planNotifications } from "./notificationPlanner";

/** 발송 회차: 미발송 이벤트 → 알림 계획 → 발행(1배치) → 소비·문자 발송 → 회차 기록 */
export class DispatchService {
  private lastRunId = 0;

  constructor(
    private readonly orders: OrderRepository,
    private readonly customers: CustomerRepository,
    private readonly publisher: AlertPublisher,
    private readonly consumer: AlertConsumer,
    private readonly log: DeliveryLogRepository,
  ) {}

  runOnce(): DispatchRun {
    const runId = ++this.lastRunId;
    const events = this.orders.takePending();
    const plan = planNotifications(events, this.customers);
    const pub = this.publisher.publishRun(plan.notifications);
    const drained = this.consumer.drain(runId);
    const run: DispatchRun = {
      runId,
      planned: plan.notifications,
      skippedOptOut: plan.skippedOptOut,
      published: pub.accepted,
      batch: pub.batch,
      delivered: drained.delivered,
      failed: drained.failed,
    };
    this.log.saveRun(run);
    return run;
  }
}
