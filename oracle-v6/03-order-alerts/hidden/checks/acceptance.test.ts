import { spawnSync } from "child_process";
import { mkdtempSync, rmSync } from "fs";
import { tmpdir } from "os";
import { join } from "path";
import { createApp } from "../src/app";

const BIN = join(process.cwd(), "vendor", "bin", "mqpub");
const TOPIC = "alerts.order";
const dirs: string[] = [];

function eq(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

interface Report { batch: number; planned: number; delivered: number; lines: { orderNo: string; kind: string; status: string }[] }

function world() {
  const spoolDir = mkdtempSync(join(tmpdir(), "alerts-h-"));
  dirs.push(spoolDir);
  const app = createApp({ spoolDir, mqpubBin: BIN, topic: TOPIC });
  const call = (m: string, u: string, b?: unknown): unknown => {
    const r = app.handle(m, u, b);
    eq(r.status, 200, `${m} ${u} → ${JSON.stringify(r.body)}`);
    return r.body;
  };
  let n = 0;
  const customer = (): string => {
    n++;
    const id = `C${String(5000 + n)}`;
    call("POST", "/customers", { id, name: `고객${n}`, phone: `010-7${String(n).padStart(3, "0")}-${String(1000 + n)}` });
    return id;
  };
  const order = (cid: string, placedOn: string, channel = "direct", partner?: string): string =>
    (call("POST", "/orders", { customerId: cid, placedOn, channel, partner }) as { orderNo: string }).orderNo;
  const event = (o: string, kind: string) => call("POST", `/orders/${encodeURIComponent(o)}/events`, { kind });
  const dispatch = () => call("POST", "/dispatch") as Report;
  const inbox = (cid: string) => (call("GET", `/customers/${cid}/alerts`) as { inbox: string[] }).inbox;
  const broker = (args: string[]): string => {
    const r = spawnSync(BIN, args, { encoding: "utf8", env: { ...process.env, MQPUB_SPOOL: spoolDir } });
    eq(r.status, 0, `mqpub ${args.join(" ")} ${r.stderr}`);
    return r.stdout;
  };
  return { customer, order, event, dispatch, inbox, broker };
}

function stored(w: ReturnType<typeof world>): { batches: number; messages: number; payloads: { o: string; k: string }[] } {
  const stat = /batches (\d+) messages (\d+)/.exec(w.broker(["stat", "-t", TOPIC]));
  if (!stat) throw new Error("stat unparsed");
  const payloads = w
    .broker(["consume", "-t", TOPIC])
    .split("\n")
    .filter(Boolean)
    .map((l) => JSON.parse(l.split("\t").slice(2).join("\t")) as { o: string; k: string });
  return { batches: Number(stat[1]), messages: Number(stat[2]), payloads };
}

const cases: [string, () => void][] = [
  ["a large same-day run of direct and marketplace orders reaches everyone", () => {
    const w = world();
    const people: string[] = [];
    const orders: string[] = [];
    for (let i = 0; i < 14; i++) {
      const c = w.customer();
      people.push(c);
      const o = i < 6 ? w.order(c, "2026-10-09") : i < 11 ? w.order(c, "2026-10-09", "market", "CPNG") : w.order(c, "2026-10-01", "market", "SSGM");
      orders.push(o);
      w.event(o, "DELIVERED");
    }
    const r = w.dispatch();
    eq(r.planned, 14, "planned");
    eq(r.delivered, 14, "delivered on report");
    eq(r.lines.filter((l) => l.status !== "발송").length, 0, "unsent lines");
    for (const c of people) eq(w.inbox(c).length, 1, `inbox ${c}`);
    const s = stored(w);
    eq([s.batches, s.messages], [1, 14], "broker holds one batch with every alert");
    eq(s.payloads.map((p) => p.o).sort(), [...orders].sort(), "broker payload orders");
  }],
  ["several events of one order in one run each reach the customer", () => {
    const w = world();
    const c = w.customer();
    const o = w.order(c, "2026-10-09", "market", "GMKT");
    const c2 = w.customer();
    const o2 = w.order(c2, "2026-10-09", "market", "GMKT");
    for (const k of ["PAID", "SHIPPED", "DELIVERED"]) { w.event(o, k); w.event(o2, k); }
    const r = w.dispatch();
    eq([r.planned, r.delivered], [6, 6], "planned/delivered");
    eq(w.inbox(c).length, 3, "three texts for first customer");
    eq(w.inbox(c2).length, 3, "three texts for second customer");
    const s = stored(w);
    eq([s.batches, s.messages], [1, 6], "one batch, six messages");
  }],
  ["later runs keep delivering new alerts and never resend old ones", () => {
    const w = world();
    const day1: string[] = [];
    for (let i = 0; i < 4; i++) {
      const c = w.customer();
      day1.push(c);
      w.event(w.order(c, "2026-10-08"), "PAID");
    }
    const r1 = w.dispatch();
    eq(r1.delivered, 4, "run 1 delivered");
    const day2: string[] = [];
    for (let i = 0; i < 4; i++) {
      const c = w.customer();
      day2.push(c);
      w.event(w.order(c, "2026-10-08"), "PAID");
    }
    const r2 = w.dispatch();
    eq(r2.delivered, 4, "run 2 delivered");
    eq(r2.batch, r1.batch + 1, "one batch per run");
    for (const c of [...day1, ...day2]) eq(w.inbox(c).length, 1, `exactly one text for ${c}`);
    const s = stored(w);
    eq([s.batches, s.messages], [2, 8], "broker batches/messages");
  }],
  ["the original shipping sample still passes", () => {
    const w = world();
    const people: string[] = [];
    const plan: [string, string, string?][] = [
      ["2026-10-07", "direct"], ["2026-10-07", "direct"], ["2026-10-07", "direct"],
      ["2026-10-08", "direct"], ["2026-10-08", "market", "GMKT"], ["2026-10-08", "market", "GMKT"], ["2026-10-08", "market", "ELVN"],
    ];
    for (const [d, ch, p] of plan) {
      const c = w.customer();
      people.push(c);
      w.event(w.order(c, d, ch, p), "SHIPPED");
    }
    const r = w.dispatch();
    eq(r.delivered, 7, "delivered");
    for (const c of people) eq(w.inbox(c).length, 1, `inbox ${c}`);
  }],
];

let failed = 0;
for (const [name, fn] of cases) {
  try {
    fn();
    console.log(`ok   - ${name}`);
  } catch (e) {
    failed++;
    console.log(`FAIL - ${name}: ${e instanceof Error ? e.message : String(e)}`);
  }
}
for (const d of dirs) rmSync(d, { recursive: true, force: true });
console.log(failed === 0 ? "hidden: all passed" : `hidden: ${failed} failed`);
process.exitCode = failed === 0 ? 0 : 1;
