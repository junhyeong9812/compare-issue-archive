import { createApp } from "../src/app";
import { FakeClock, eq, run, test } from "./harness";

interface Row { id: string; status: { label: string; tone: string }; payment: { label: string } | null; shipment: { label: string } | null }

function place(app: ReturnType<typeof createApp>, customer: string): string {
  const res = app.handle("POST", "/orders", { customer, lines: [{ sku: "SKU-1", qty: 1, unitPrice: 12000 }] });
  eq(res.status, 200, `create ${customer}`);
  return (res.body as { id: string }).id;
}

test("a freshly placed order is shown as waiting for acceptance", () => {
  const app = createApp(new FakeClock());
  const id = place(app, "park");
  const row = app.handle("GET", `/orders/${id}`).body as Row;
  eq(row.status, { label: "접수 대기", tone: "warn" }, "status column");
  eq(row.payment, null, "no payment yet");
});

test("order list shows each column with its own wording", () => {
  const clock = new FakeClock();
  const app = createApp(clock);
  const id = place(app, "choi");
  clock.advance(1000);
  app.handle("POST", `/orders/${id}/payment`, { status: "PENDING", amount: 12000 });
  const page = app.handle("GET", "/orders").body as { items: Row[] };
  eq(page.items[0].status.label, "접수 대기", "order column");
  eq(page.items[0].payment?.label, "결제 대기", "payment column");
});

test("dashboard counts unaccepted orders as needing attention", () => {
  const clock = new FakeClock();
  const app = createApp(clock);
  const a = place(app, "a");
  const b = place(app, "b");
  const c = place(app, "c");
  app.handle("POST", `/orders/${b}/status`, { status: "CONFIRMED" });
  app.handle("POST", `/orders/${b}/payment`, { status: "CAPTURED", amount: 12000 });
  app.handle("POST", `/orders/${c}/status`, { status: "CANCELLED" });
  const summary = app.handle("GET", "/dashboard").body as { needsAttention: number; attentionIds: string[] };
  eq(summary.attentionIds, [a], "orders needing attention");
});

run();
