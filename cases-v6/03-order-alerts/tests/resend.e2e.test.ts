import { mkdtempSync, rmSync } from "fs";
import { tmpdir } from "os";
import { join } from "path";
import { createApp } from "../src/app";
import { eq, onCleanup, run, test } from "./harness";

interface Report {
  runId: number;
  planned: number;
  delivered: number;
  failed: number;
  lines: { orderNo: string; kind: string; status: string; reason?: string }[];
}

/** 모의 문자 게이트웨이에 번호별 실패 응답을 넣은 앱 하나 */
function world(smsFaults: Record<string, string[]>) {
  const spoolDir = mkdtempSync(join(tmpdir(), "alerts-r-"));
  onCleanup(() => rmSync(spoolDir, { recursive: true, force: true }));
  const app = createApp({ spoolDir, mqpubBin: join(process.cwd(), "vendor", "bin", "mqpub"), smsFaults });
  const call = (method: string, url: string, body?: unknown): unknown => {
    const r = app.handle(method, url, body);
    eq(r.status, 200, `${method} ${url} → ${JSON.stringify(r.body)}`);
    return r.body;
  };
  return {
    customer: (id: string, name: string, phone: string) => call("POST", "/customers", { id, name, phone }),
    order: (customerId: string, placedOn: string) =>
      (call("POST", "/orders", { customerId, placedOn }) as { orderNo: string }).orderNo,
    event: (orderNo: string, kind: string) => call("POST", `/orders/${encodeURIComponent(orderNo)}/events`, { kind }),
    dispatch: () => call("POST", "/dispatch") as Report,
    inbox: (id: string) => (call("GET", `/customers/${id}/alerts`) as { inbox: string[] }).inbox,
  };
}

test("an alert that failed with E-THROTTLE goes out in the next dispatch run", () => {
  const w = world({ "010-5100-0001": ["E-THROTTLE"] });
  w.customer("C5101", "문가온", "010-5100-0001");
  w.customer("C5102", "배하린", "010-5100-0002");
  const a = w.order("C5101", "2026-10-09");
  const b = w.order("C5102", "2026-10-09");
  w.event(a, "SHIPPED");
  w.event(b, "SHIPPED");

  const r1 = w.dispatch();
  eq(r1.lines.find((l) => l.orderNo === a)?.status, "실패", "first run: throttled alert failed");
  eq(r1.lines.find((l) => l.orderNo === b)?.status, "발송", "first run: other alert sent");
  eq(w.inbox("C5101").length, 0, "nothing received yet");

  const r2 = w.dispatch();
  eq(r2.lines.filter((l) => l.orderNo === a).map((l) => l.status), ["발송"], "second run: the failed alert is sent");
  eq(w.inbox("C5101").length, 1, "customer got the shipping text");
  eq(w.inbox("C5101")[0].includes("출발"), true, "shipping wording");
  eq(w.inbox("C5102").length, 1, "other customer is not texted again");

  const r3 = w.dispatch();
  eq(r3.planned, 0, "third run: nothing left to send");
  eq(w.inbox("C5101").length, 1, "still one text");
});

run();
