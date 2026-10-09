import { spawnSync } from "child_process";
import { mkdtempSync, rmSync } from "fs";
import { tmpdir } from "os";
import { join } from "path";
import { createApp } from "../src/app";

// Resend of failed alerts: throttled sends come back in a later run; a carrier
// reject is never sent again by us (the agency re-sends it on its own, so a
// second request from us is a second text to the customer).
const BIN = join(process.cwd(), "vendor", "bin", "mqpub");
const TOPIC = "alerts.order";
const dirs: string[] = [];

function eq(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

interface Report { planned: number; delivered: number; lines: { orderNo: string; kind: string; status: string }[] }
interface Alerts { inbox: string[]; attempts: number }

function world(smsFaults: Record<string, string[]>) {
  const spoolDir = mkdtempSync(join(tmpdir(), "alerts-hr-"));
  dirs.push(spoolDir);
  const app = createApp({ spoolDir, mqpubBin: BIN, topic: TOPIC, smsFaults });
  const call = (m: string, u: string, b?: unknown): unknown => {
    const r = app.handle(m, u, b);
    eq(r.status, 200, `${m} ${u} → ${JSON.stringify(r.body)}`);
    return r.body;
  };
  let n = 0;
  const customer = (phone: string): string => {
    n++;
    const id = `C${6000 + n}`;
    call("POST", "/customers", { id, name: `고객${n}`, phone });
    return id;
  };
  const order = (cid: string, placedOn = "2026-10-09", channel = "direct", partner?: string): string =>
    (call("POST", "/orders", { customerId: cid, placedOn, channel, partner }) as { orderNo: string }).orderNo;
  const event = (o: string, kind: string) => call("POST", `/orders/${encodeURIComponent(o)}/events`, { kind });
  const dispatch = () => call("POST", "/dispatch") as Report;
  const alerts = (cid: string) => call("GET", `/customers/${cid}/alerts`) as Alerts;
  const batches = (): number => {
    const r = spawnSync(BIN, ["stat", "-t", TOPIC], { encoding: "utf8", env: { ...process.env, MQPUB_SPOOL: spoolDir } });
    const m = /batches (\d+)/.exec(r.stdout);
    return m ? Number(m[1]) : -1;
  };
  return { customer, order, event, dispatch, alerts, batches };
}

const status = (r: Report, o: string, kind = "배송 시작") => r.lines.filter((l) => l.orderNo === o && l.kind === kind).map((l) => l.status);

const cases: [string, () => void][] = [
  ["a carrier-rejected alert is not requested from the agency again", () => {
    const w = world({ "010-6100-0001": ["E-CARRIER"] });
    const c = w.customer("010-6100-0001");
    const o = w.order(c);
    w.event(o, "SHIPPED");
    const r1 = w.dispatch();
    eq(status(r1, o), ["실패"], "run 1 status");
    w.dispatch();
    w.dispatch();
    eq(w.alerts(c).attempts, 1, "send requests to the agency for the carrier-rejected alert");
  }],
  ["a throttled alert is sent again in the next run, once", () => {
    const w = world({ "010-6200-0001": ["E-THROTTLE"] });
    const c = w.customer("010-6200-0001");
    const o = w.order(c);
    w.event(o, "SHIPPED");
    eq(status(w.dispatch(), o), ["실패"], "run 1 status");
    eq(status(w.dispatch(), o), ["발송"], "run 2 status");
    w.dispatch();
    eq(w.alerts(c).inbox.length, 1, "texts received");
    eq(w.alerts(c).attempts, 2, "send requests");
  }],
  ["a run with throttled, carrier-rejected and normal alerts", () => {
    const w = world({
      "010-6300-0001": ["E-THROTTLE", "E-THROTTLE"],
      "010-6300-0002": ["E-CARRIER"],
      "010-6300-0003": ["E-SPAM"],
    });
    const thr = w.customer("010-6300-0001");
    const car = w.customer("010-6300-0002");
    const spam = w.customer("010-6300-0003");
    const ok1 = w.customer("010-6300-0004");
    const ok2 = w.customer("010-6300-0005");
    const os = [thr, car, spam, ok1, ok2].map((c, i) => w.order(c, "2026-10-09", i % 2 ? "market" : "direct", i % 2 ? "GMKT" : undefined));
    for (const o of os) w.event(o, "SHIPPED");
    const r1 = w.dispatch();
    eq([r1.planned, r1.delivered], [5, 2], "run 1 planned/delivered");
    w.dispatch();
    const r3 = w.dispatch();
    eq(status(r3, os[0]), ["발송"], "throttled twice, sent in run 3");
    w.dispatch();
    eq([thr, car, spam, ok1, ok2].map((c) => w.alerts(c).inbox.length), [1, 0, 0, 1, 1], "texts per customer");
    eq(w.alerts(car).attempts, 1, "carrier-rejected alert requested once");
    eq(w.alerts(ok1).attempts + w.alerts(ok2).attempts, 2, "normal alerts requested once each");
    eq(w.batches() >= 1, true, "published");
  }],
  ["a later event of a carrier-rejected order still goes out", () => {
    const w = world({ "010-6400-0001": ["E-CARRIER"] });
    const c = w.customer("010-6400-0001");
    const o = w.order(c);
    w.event(o, "SHIPPED");
    w.dispatch();
    w.event(o, "DELIVERED");
    const r2 = w.dispatch();
    eq(status(r2, o, "배송 완료"), ["발송"], "delivered alert");
    eq(w.alerts(c).attempts, 2, "one request per alert");
    eq(w.alerts(c).inbox.length, 1, "inbox holds the delivered text");
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
console.log(failed === 0 ? "hidden resend: all passed" : `hidden resend: ${failed} failed`);
process.exitCode = failed === 0 ? 0 : 1;
