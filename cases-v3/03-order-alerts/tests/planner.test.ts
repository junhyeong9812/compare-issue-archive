import { CustomerRepository } from "../src/repository/customerRepository";
import { OrderRepository } from "../src/repository/orderRepository";
import { planNotifications } from "../src/service/notificationPlanner";
import { eq, run, test } from "./harness";

function setup() {
  const customers = new CustomerRepository();
  customers.save({ id: "C1", name: "가", phone: "010-1111-0001", smsOptIn: true });
  customers.save({ id: "C2", name: "나", phone: "010-1111-0002", smsOptIn: false });
  const orders = new OrderRepository();
  return { customers, orders };
}

test("repeated identical events in one run produce one alert", () => {
  const { customers, orders } = setup();
  const o = orders.create("C1", "direct", "2026-10-09");
  orders.addEvent(o.orderNo, "SHIPPED");
  orders.addEvent(o.orderNo, "SHIPPED");
  orders.addEvent(o.orderNo, "DELIVERED");
  const plan = planNotifications(orders.takePending(), customers);
  eq(plan.notifications.map((n) => n.kind), ["SHIPPED", "DELIVERED"], "kinds");
});

test("opt-out customers are skipped and counted", () => {
  const { customers, orders } = setup();
  orders.addEvent(orders.create("C2", "direct", "2026-10-09").orderNo, "PAID");
  orders.addEvent(orders.create("C1", "direct", "2026-10-09").orderNo, "PAID");
  const plan = planNotifications(orders.takePending(), customers);
  eq(plan.skippedOptOut, 1, "skipped");
  eq(plan.notifications.map((n) => n.customerId), ["C1"], "kept");
});

test("pending events are taken once, in recorded order", () => {
  const { orders } = setup();
  const a = orders.create("C1", "direct", "2026-10-09").orderNo;
  const b = orders.create("C1", "market", "2026-10-09", "ELVN").orderNo;
  orders.addEvent(b, "PAID");
  orders.addEvent(a, "PAID");
  eq(orders.takePending().map((e) => e.orderNo), [b, a], "order");
  eq(orders.takePending(), [], "second take");
});

run();
