import { OrderStatus } from "../src/domain/order";
import { PaymentStatus } from "../src/domain/payment";
import { OrderRepository } from "../src/repository/orderRepository";
import { PaymentRepository } from "../src/repository/paymentRepository";
import { ShipmentRepository } from "../src/repository/shipmentRepository";
import { OrderCommandService } from "../src/service/orderCommandService";
import { OrderQueryService } from "../src/service/orderQueryService";
import { FakeClock, eq, run, test } from "./harness";

function setup() {
  const clock = new FakeClock();
  const orders = new OrderRepository();
  const payments = new PaymentRepository();
  const shipments = new ShipmentRepository();
  return {
    clock,
    payments,
    cmd: new OrderCommandService(orders, payments, shipments, clock),
    query: new OrderQueryService(orders, payments, shipments),
  };
}

test("orders move only along allowed transitions", () => {
  const { cmd } = setup();
  const o = cmd.place("kim", [{ sku: "A", qty: 1, unitPrice: 1000 }]);
  cmd.transition(o.id, OrderStatus.Confirmed);
  let msg = "";
  try { cmd.transition(o.id, OrderStatus.Pending); } catch (e) { msg = (e as Error).message; }
  eq(msg, "cannot move CONFIRMED -> PENDING", "rejected");
});

test("latest payment attempt wins", () => {
  const { cmd, clock, query, payments } = setup();
  const o = cmd.place("lee", [{ sku: "B", qty: 2, unitPrice: 500 }]);
  cmd.recordPayment(o.id, PaymentStatus.Failed, 1000);
  clock.advance(1000);
  cmd.recordPayment(o.id, PaymentStatus.Captured, 1000);
  eq(query.byId(o.id)?.payment?.status, "CAPTURED", "latest");
  eq(payments.attempts(o.id), 2, "attempts");
});

test("pending orders are listed first, newest first", () => {
  const { cmd, clock, query } = setup();
  const a = cmd.place("a", [{ sku: "X", qty: 1, unitPrice: 1 }]);
  clock.advance(10);
  const b = cmd.place("b", [{ sku: "X", qty: 1, unitPrice: 1 }]);
  cmd.transition(a.id, OrderStatus.Confirmed);
  clock.advance(10);
  const c = cmd.place("c", [{ sku: "X", qty: 1, unitPrice: 1 }]);
  eq(query.find().map((r) => r.order.id), [c.id, b.id, a.id], "order");
});

run();
