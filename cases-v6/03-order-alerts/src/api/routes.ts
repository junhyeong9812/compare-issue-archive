import { OrderService, PlaceOrderInput } from "../service/orderService";
import { OrderRepository } from "../repository/orderRepository";
import { buildDispatchReport } from "../report/dispatchReport";
import { CustomerRepository } from "../repository/customerRepository";
import { DeliveryLogRepository } from "../repository/deliveryLogRepository";
import { DispatchService } from "../service/dispatchService";
import { InMemorySmsGateway } from "../notifier/smsGateway";
import { Router, notFound, ok } from "./router";

export function mountOrderRoutes(router: Router, service: OrderService, orders: OrderRepository): void {
  router
    .add("POST", "/customers", (_p, body) => ok(service.registerCustomer(body)))
    .add("POST", "/orders", (_p, body) => {
      const o = service.placeOrder(body as PlaceOrderInput);
      return ok({ orderNo: o.orderNo });
    })
    .add("GET", "/orders/:orderNo", (p) => {
      const o = orders.find(p.orderNo);
      return o ? ok(o) : notFound("order");
    })
    .add("POST", "/orders/:orderNo/events", (p, body) => {
      const kind = (body as { kind?: unknown } | undefined)?.kind;
      return ok(service.recordEvent(p.orderNo, kind));
    });
}

export function mountAlertRoutes(
  router: Router,
  dispatch: DispatchService,
  log: DeliveryLogRepository,
  customers: CustomerRepository,
  gateway: InMemorySmsGateway,
): void {
  router
    .add("POST", "/dispatch", () => {
      const run = dispatch.runOnce();
      return ok(buildDispatchReport(run, customers));
    })
    .add("GET", "/reports/dispatch/:runId", (p) => {
      const run = log.findRun(Number(p.runId));
      return run ? ok(buildDispatchReport(run, customers)) : notFound("run");
    })
    .add("GET", "/customers/:id/alerts", (p) => {
      const c = customers.find(p.id);
      if (!c) return notFound("customer");
      return ok({
        customerId: c.id,
        delivered: log.byCustomer(c.id),
        inbox: gateway.inbox(c.phone),
        attempts: gateway.attempts(c.phone),
      });
    });
}
