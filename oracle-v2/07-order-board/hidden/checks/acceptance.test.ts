import { createApp } from "../src/app";

declare const process: { exitCode?: number };

type B = { label: string; tone: string } | null;
interface Row { id: string; status: { label: string; tone: string }; payment: B; shipment: B }

function eq(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

class Clock { t = 1_800_000_000_000; now(): number { return this.t; } }

function world() {
  const clock = new Clock();
  const app = createApp(clock, { pageSize: 50 });
  const place = (customer: string): string => {
    clock.t += 1000;
    const r = app.handle("POST", "/orders", { customer, lines: [{ sku: "Q-9", qty: 3, unitPrice: 3300 }] });
    eq(r.status, 200, "create");
    return (r.body as { id: string }).id;
  };
  const post = (url: string, body: unknown) => { clock.t += 1000; eq(app.handle("POST", url, body).status, 200, url); };
  const row = (id: string) => app.handle("GET", `/orders/${id}`).body as Row;
  return { app, place, post, row };
}

const cases: [string, () => void][] = [
  ["every column uses its own domain wording", () => {
    const w = world();
    const a = w.place("a");
    w.post(`/orders/${a}/payment`, { status: "PENDING", amount: 9900 });
    eq(w.row(a).status, { label: "접수 대기", tone: "warn" }, "order pending");
    eq(w.row(a).payment, { label: "결제 대기", tone: "neutral" }, "payment pending");

    const b = w.place("b");
    w.post(`/orders/${b}/status`, { status: "CONFIRMED" });
    w.post(`/orders/${b}/payment`, { status: "FAILED", amount: 9900 });
    w.post(`/orders/${b}/shipment`, { status: "FAILED" });
    eq(w.row(b).status, { label: "주문 확정", tone: "info" }, "order confirmed");
    eq(w.row(b).payment, { label: "결제 실패", tone: "danger" }, "payment failed");
    eq(w.row(b).shipment, { label: "배송 실패", tone: "danger" }, "shipment failed");

    const c = w.place("c");
    w.post(`/orders/${c}/status`, { status: "CONFIRMED" });
    w.post(`/orders/${c}/shipment`, { status: "RETURNED" });
    eq(w.row(c).shipment, { label: "반송", tone: "warn" }, "shipment returned");
    eq(w.row(c).payment, null, "no payment");
  }],
  ["shipment failure keeps delivery wording even after payment succeeds", () => {
    const w = world();
    const id = w.place("d");
    w.post(`/orders/${id}/status`, { status: "CONFIRMED" });
    w.post(`/orders/${id}/payment`, { status: "CAPTURED", amount: 9900 });
    w.post(`/orders/${id}/shipment`, { status: "FAILED" });
    eq(w.row(id).shipment?.label, "배송 실패", "shipment");
    eq(w.row(id).payment?.label, "결제 완료", "payment");
  }],
  ["filtered list of new orders", () => {
    const w = world();
    const ids = [w.place("x"), w.place("y"), w.place("z")];
    w.post(`/orders/${ids[1]}/status`, { status: "CONFIRMED" });
    const page = w.app.handle("GET", "/orders?status=PENDING").body as { items: Row[] };
    eq(page.items.map((r) => r.id), [ids[2], ids[0]], "ids");
    eq(page.items.map((r) => r.status.label), ["접수 대기", "접수 대기"], "labels");
  }],
  ["dashboard attention covers order, payment and shipment problems", () => {
    const w = world();
    const fresh = w.place("fresh");
    const paidOk = w.place("ok");
    w.post(`/orders/${paidOk}/status`, { status: "CONFIRMED" });
    w.post(`/orders/${paidOk}/payment`, { status: "CAPTURED", amount: 9900 });
    w.post(`/orders/${paidOk}/shipment`, { status: "DELIVERED" });
    const lost = w.place("lost");
    w.post(`/orders/${lost}/status`, { status: "CONFIRMED" });
    w.post(`/orders/${lost}/shipment`, { status: "FAILED" });
    const declined = w.place("declined");
    w.post(`/orders/${declined}/status`, { status: "CONFIRMED" });
    w.post(`/orders/${declined}/payment`, { status: "FAILED", amount: 9900 });
    const waitingPay = w.place("waiting");
    w.post(`/orders/${waitingPay}/status`, { status: "CONFIRMED" });
    w.post(`/orders/${waitingPay}/payment`, { status: "PENDING", amount: 9900 });
    const s = w.app.handle("GET", "/dashboard").body as { needsAttention: number; attentionIds: string[]; byTone: Record<string, number> };
    eq([...s.attentionIds].sort(), [fresh, lost, declined].sort(), "attention ids");
    eq(s.byTone.warn ?? 0, 1, "warn tone count");
    eq(s.byTone.info ?? 0, 4, "info tone count");
  }],
];

let failed = 0;
for (const [name, fn] of cases) {
  try { fn(); console.log(`ok   - ${name}`); }
  catch (e) { failed++; console.log(`FAIL - ${name}: ${e instanceof Error ? e.message : String(e)}`); }
}
console.log(failed === 0 ? "all passed" : `${failed} failed`);
process.exitCode = failed === 0 ? 0 : 1;
