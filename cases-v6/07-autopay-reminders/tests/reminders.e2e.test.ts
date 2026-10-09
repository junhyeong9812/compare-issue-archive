import { mkdtempSync, rmSync } from "fs";
import { tmpdir } from "os";
import { join } from "path";
import { App, createApp } from "../src/app";
import { assert, eq, onCleanup, run, test } from "./harness";

interface Report {
  planned: number;
  delivered: number;
  missing: number;
  pending: number;
  lines: { key: string; memberId: string; kind: string; status: string; sendAt: string }[];
  text: string;
}

interface Notifications {
  memberId: string;
  notifications: { at: string; tag: string; message: string }[];
}

/** 빈 pushq 상태 디렉토리를 가진 앱 하나 */
function world() {
  const home = mkdtempSync(join(tmpdir(), "autopay-rmd-"));
  onCleanup(() => rmSync(home, { recursive: true, force: true }));
  const app: App = createApp({ pushqHome: home, pushqBin: join(process.cwd(), "vendor", "bin", "pushq") });

  const call = (method: string, url: string, body?: unknown): unknown => {
    const r = app.handle(method, url, body);
    eq(r.status, 200, `${method} ${url} → ${JSON.stringify(r.body)}`);
    return r.body;
  };

  return {
    member(id: string, name: string, device: string, leadMinutes: number, pushOptIn = true): void {
      call("POST", "/members", { id, name, device, leadMinutes, pushOptIn });
    },
    autopay(memberId: string, merchant: string, amount: number, billingDay: number, billingTime: string): string {
      return (call("POST", "/autopays", { memberId, merchant, amount, billingDay, billingTime }) as { id: string }).id;
    },
    plan(date: string): { planned: number; scheduled: number; alreadyScheduled: number; skippedOptOut: number } {
      return call("POST", "/reminders/plan", { date }) as { planned: number; scheduled: number; alreadyScheduled: number; skippedOptOut: number };
    },
    fail(autopayId: string, failedAt: string): void {
      call("POST", `/autopays/${autopayId}/failures`, { failedAt });
    },
    cancel(autopayId: string): void {
      call("POST", `/autopays/${autopayId}/cancel`);
    },
    clock(until: string): void {
      call("POST", "/clock", { until });
    },
    report(date: string): Report {
      return call("GET", `/reports/reminders/${date}`) as Report;
    },
    inbox(memberId: string): Notifications["notifications"] {
      return (call("GET", `/members/${memberId}/notifications`) as Notifications).notifications;
    },
  };
}

test("every autopay customer gets the withdrawal reminder in time on billing day", () => {
  const w = world();
  w.member("M1001", "김하늘", "ios:Hb7cQ2xk91ZpA", 30);
  w.member("M1002", "이도윤", "and:fT0p3LmQw8eR", 60);
  w.member("M1003", "박서연", " IOS:Zx81kQpLm0Ty ", 30);
  w.member("M1004", "최민준", "and:Pq19sXkLm2Vb", 10);
  w.member("M1005", "정지우", "ios:Nn41tRe7Ku3Q", 1440);

  const phone = w.autopay("M1001", "한빛모바일 통신요금", 55000, 12, "08:00");
  w.autopay("M1002", "든든손해보험", 89000, 12, "10:20");
  w.autopay("M1003", "스트림플러스 구독", 13900, 12, "13:47");
  w.autopay("M1004", "푸른마을 관리비", 187420, 12, "18:33");
  w.autopay("M1005", "새싹수학학원", 320000, 12, "09:00");

  const plan = w.plan("2026-10-12");
  eq([plan.planned, plan.scheduled], [5, 5], "reminders planned for the 12th");

  // 08:00 출금이 잔액 부족으로 실패 → 2시간 뒤 재출금, 그 30분 전에 안내
  w.clock("2026-10-12T08:00:37");
  w.fail(phone, "2026-10-12T08:00:37");
  w.clock("2026-10-12T23:59:00");

  const r = w.report("2026-10-12");
  eq(r.lines.filter((l) => l.status !== "발송").map((l) => `${l.memberId} ${l.kind}`), [], "reminders not delivered");
  eq([r.planned, r.delivered, r.missing], [6, 6, 0], `report counts (${r.text})`);

  // 고객이 설정한 시점보다 늦게 도착하면 안 된다
  const latest: [string, string[], string][] = [
    ["M1001", ["2026-10-12T07:30:00", "2026-10-12T09:30:37"], "한빛모바일"],
    ["M1002", ["2026-10-12T09:20:00"], "든든손해보험"],
    ["M1003", ["2026-10-12T13:17:00"], "스트림플러스"],
    ["M1004", ["2026-10-12T18:23:00"], "푸른마을"],
    ["M1005", ["2026-10-11T09:00:00"], "새싹수학학원"],
  ];
  for (const [id, deadlines, merchant] of latest) {
    const inbox = w.inbox(id);
    eq(inbox.length, deadlines.length, `pushes received by ${id}`);
    inbox.forEach((n, i) => {
      assert(n.at <= deadlines[i], `${id} push #${i + 1} arrived at ${n.at}, later than ${deadlines[i]}`);
      assert(n.message.includes(merchant), `${id} push mentions ${merchant}`);
    });
  }
});

test("a retry that fails again gets its own notice before the second retry", () => {
  const w = world();
  w.member("M4001", "강예린", "ios:Rt55uV66wW77", 30);
  const gas = w.autopay("M4001", "도시가스 요금", 48200, 15, "08:00");
  w.plan("2026-10-15");

  w.clock("2026-10-15T08:00:41");
  w.fail(gas, "2026-10-15T08:00:41"); // 재출금 10:00:41
  w.clock("2026-10-15T10:01:12");
  w.fail(gas, "2026-10-15T10:01:12"); // 재출금도 실패 → 2차 재출금 12:01:12
  w.clock("2026-10-15T23:00:00");

  const inbox = w.inbox("M4001");
  eq(inbox.length, 3, "withdrawal reminder + two retry notices");
  const deadlines = ["2026-10-15T07:30:00", "2026-10-15T09:30:41", "2026-10-15T11:31:12"];
  inbox.forEach((n, i) => assert(n.at <= deadlines[i], `push #${i + 1} arrived at ${n.at}, later than ${deadlines[i]}`));
  assert(inbox[1].message.includes("10:00"), `first retry notice names 10:00: ${inbox[1].message}`);
  assert(inbox[2].message.includes("12:01"), `second retry notice names 12:01: ${inbox[2].message}`);

  const r = w.report("2026-10-15");
  eq([r.planned, r.delivered, r.missing], [3, 3, 0], `report counts (${r.text})`);
  eq(r.lines.filter((l) => l.kind === "재출금 안내").length, 2, "retry notice lines");
});

test("customers who turned pushes off are skipped and counted", () => {
  const w = world();
  w.member("M2001", "한지민", "ios:Aa11bB22cC33", 30);
  w.member("M2002", "오세훈", "and:Dd44eE55fF66", 30, false);
  w.autopay("M2001", "든든손해보험", 42000, 3, "09:00");
  w.autopay("M2002", "든든손해보험", 42000, 3, "09:00");
  const plan = w.plan("2026-11-03");
  eq([plan.planned, plan.skippedOptOut], [1, 1], "planned / opted out");
  w.clock("2026-11-03T12:00:00");
  eq(w.inbox("M2001").length, 1, "opted-in customer");
  eq(w.inbox("M2002"), [], "opted-out customer");
});

test("planning the same day twice does not double-book, and cancelled contracts stay quiet", () => {
  const w = world();
  w.member("M3001", "서지호", "ios:Gg77hH88iI99", 60);
  w.member("M3002", "윤시우", "and:Jj00kK11lL22", 30);
  w.autopay("M3001", "한빛모바일 통신요금", 61000, 20, "11:00");
  const gym = w.autopay("M3002", "바른필라테스", 99000, 20, "07:30");
  eq(w.plan("2026-10-20").scheduled, 2, "first planning");
  const again = w.plan("2026-10-20");
  eq([again.scheduled, again.alreadyScheduled], [0, 2], "second planning");
  w.cancel(gym);
  w.clock("2026-10-20T23:00:00");
  eq(w.inbox("M3001").length, 1, "one reminder, not two");
  eq(w.inbox("M3002"), [], "cancelled contract");
});

run();
