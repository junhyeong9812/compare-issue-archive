import { mkdtempSync, rmSync } from "fs";
import { tmpdir } from "os";
import { join } from "path";
import { App, createApp } from "../src/app";
import { eq, onCleanup, run, test } from "./harness";

interface Report {
  runId: number;
  batch: number;
  planned: number;
  published: number;
  delivered: number;
  failed: number;
  skippedOptOut: number;
  lines: { orderNo: string; customerId: string; kind: string; status: string; to: string }[];
  text: string;
}

interface CustomerAlerts {
  customerId: string;
  delivered: { orderNo: string; kind: string }[];
  inbox: string[];
}

/** 빈 스풀 디렉토리를 가진 앱 하나 */
function world() {
  const spoolDir = mkdtempSync(join(tmpdir(), "alerts-"));
  onCleanup(() => rmSync(spoolDir, { recursive: true, force: true }));
  const app: App = createApp({ spoolDir, mqpubBin: join(process.cwd(), "vendor", "bin", "mqpub") });

  const call = (method: string, url: string, body?: unknown): unknown => {
    const r = app.handle(method, url, body);
    eq(r.status, 200, `${method} ${url} → ${JSON.stringify(r.body)}`);
    return r.body;
  };

  return {
    app,
    spoolDir,
    customer(id: string, name: string, phone: string, smsOptIn = true): void {
      call("POST", "/customers", { id, name, phone, smsOptIn });
    },
    order(customerId: string, placedOn: string, channel: "direct" | "market" = "direct", partner?: string): string {
      return (call("POST", "/orders", { customerId, placedOn, channel, partner }) as { orderNo: string }).orderNo;
    },
    event(orderNo: string, kind: string): void {
      call("POST", `/orders/${encodeURIComponent(orderNo)}/events`, { kind });
    },
    dispatch(): Report {
      return call("POST", "/dispatch") as Report;
    },
    report(runId: number): Report {
      return call("GET", `/reports/dispatch/${runId}`) as Report;
    },
    alerts(customerId: string): CustomerAlerts {
      return call("GET", `/customers/${customerId}/alerts`) as CustomerAlerts;
    },
  };
}

test("every customer whose order shipped today gets a shipping text", () => {
  const w = world();
  const shoppers: [string, string, string][] = [
    ["C1001", "김하늘", "010-2000-1001"],
    ["C1002", "이도윤", "010-2000-1002"],
    ["C1003", "박서연", "010-2000-1003"],
    ["C1004", "최민준", "+82 10-2000-1004"],
    ["C1005", "정지우", "010 2000 1005"],
    ["C1006", "강예린", "010-2000-1006"],
    ["C1007", "윤시우", "010-2000-1007"],
  ];
  for (const [id, name, phone] of shoppers) w.customer(id, name, phone);

  const orders = [
    w.order("C1001", "2026-10-07"),
    w.order("C1002", "2026-10-07"),
    w.order("C1003", "2026-10-07"),
    w.order("C1004", "2026-10-08"),
    w.order("C1005", "2026-10-08", "market", "GMKT"),
    w.order("C1006", "2026-10-08", "market", "GMKT"),
    w.order("C1007", "2026-10-08", "market", "ELVN"),
  ];
  for (const o of orders) w.event(o, "SHIPPED");

  const report = w.dispatch();
  eq(report.planned, 7, "planned alerts");
  eq(report.lines.filter((l) => l.status !== "발송").map((l) => l.orderNo), [], "alerts not sent");
  eq(report.delivered, 7, "sent count on the dispatch report");

  for (const [id] of shoppers) {
    const a = w.alerts(id);
    eq(a.inbox.length, 1, `texts received by ${id}`);
    eq(a.inbox[0].includes("출발"), true, `shipping wording for ${id}`);
  }
});

test("opted-out customers are not texted and are counted separately", () => {
  const w = world();
  w.customer("C2001", "한지민", "010-3000-2001");
  w.customer("C2002", "오세훈", "010-3000-2002", false);
  w.event(w.order("C2001", "2026-10-09"), "PAID");
  w.event(w.order("C2002", "2026-10-09"), "PAID");
  const report = w.dispatch();
  eq(report.skippedOptOut, 1, "opted out");
  eq(report.delivered, 1, "sent");
  eq(w.alerts("C2002").inbox, [], "no text for opted-out");
});

test("each dispatch run goes out as one batch and nothing is sent twice", () => {
  const w = world();
  w.customer("C3001", "서지호", "010-4000-3001");
  const o = w.order("C3001", "2026-10-09");
  w.event(o, "PAID");
  const r1 = w.dispatch();
  w.event(o, "SHIPPED");
  const r2 = w.dispatch();
  const r3 = w.dispatch();
  eq([r1.batch, r2.batch], [1, 2], "batch per run");
  eq(r3.planned, 0, "nothing new");
  eq(w.alerts("C3001").inbox.length, 2, "two texts total");
  eq(w.report(1).delivered, 1, "stored report for run 1");
});

run();
