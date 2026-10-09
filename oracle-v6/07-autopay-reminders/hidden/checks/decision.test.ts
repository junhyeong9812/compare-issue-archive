import { spawnSync } from "child_process";
import { mkdtempSync, rmSync } from "fs";
import { tmpdir } from "os";
import { join } from "path";
import { createApp } from "../src/app";

const BIN = join(process.cwd(), "vendor", "bin", "pushq");
const dirs: string[] = [];

function eq(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

function world() {
  const home = mkdtempSync(join(tmpdir(), "autopay-d-"));
  dirs.push(home);
  const app = createApp({ pushqHome: home, pushqBin: BIN });
  const call = (m: string, u: string, b?: unknown): unknown => {
    const r = app.handle(m, u, b);
    eq(r.status, 200, `${m} ${u} → ${JSON.stringify(r.body)}`);
    return r.body;
  };
  const vendor = (args: string[]): string[] => {
    const r = spawnSync(BIN, args, { encoding: "utf8", env: { ...process.env, PUSHQ_HOME: home } });
    eq(r.status, 0, `pushq ${args.join(" ")} ${r.stderr}`);
    return r.stdout.split("\n").filter(Boolean);
  };
  return {
    member: (id: string, device: string, lead: number) => call("POST", "/members", { id, name: `고객${id}`, device, leadMinutes: lead }),
    autopay: (memberId: string, merchant: string, day: number, time: string): string =>
      (call("POST", "/autopays", { memberId, merchant, amount: 30000, billingDay: day, billingTime: time }) as { id: string }).id,
    plan: (date: string) => call("POST", "/reminders/plan", { date }),
    /** 시계를 그 시각까지 진행하고 그 시각의 실패 통보를 받는다 */
    failAt: (id: string, at: string) => {
      call("POST", "/clock", { until: at });
      call("POST", `/autopays/${id}/failures`, { failedAt: at });
    },
    clock: (until: string) => call("POST", "/clock", { until }),
    report: (date: string) => call("GET", `/reports/reminders/${date}`) as { planned: number; delivered: number; missing: number; text: string },
    /** 벤더가 그 기기로 실제로 내보낸 푸시: [시각, 메시지] */
    sentTo: (device: string): [string, string][] =>
      vendor(["log", "--to", device]).map((l) => {
        const [at, , , ...msg] = l.split("\t");
        return [at, msg.join("\t")];
      }),
    sentAll: () => vendor(["log"]),
    queued: () => vendor(["list"]),
  };
}

const cases: [string, () => void][] = [
  ["one retry notice per scheduled retry — re-sent failure reports do not add notices, real repeat failures do", () => {
    const w = world();
    const date = "2026-12-03";
    const A = "ios:Rs0001aBcDeF";
    const B = "and:Rs0002gHiJkL";
    const C = "ios:Rs0003mNoPqR";
    w.member("M8101", A, 30);
    w.member("M8102", B, 10);
    w.member("M8103", C, 60);
    const a = w.autopay("M8101", "한빛모바일 통신요금", 3, "08:00");
    const b = w.autopay("M8102", "든든손해보험", 3, "13:47");
    w.autopay("M8103", "새싹수학학원", 3, "09:13");
    w.plan(date);

    w.failAt(a, `${date}T08:00:37`); // 재출금 10:00:37
    w.failAt(a, `${date}T08:03:52`); // 같은 실패의 재통보
    w.failAt(a, `${date}T08:09:15`); // 같은 실패의 재통보
    w.failAt(a, `${date}T10:00:58`); // 재출금 실패 → 12:00:58
    w.failAt(a, `${date}T10:05:20`); // 재통보
    w.failAt(a, `${date}T12:01:30`); // 재출금 실패 → 14:01:30
    w.failAt(b, `${date}T13:47:20`); // 재출금 15:47:20
    w.failAt(b, `${date}T13:52:41`); // 재통보
    w.failAt(b, `${date}T13:58:03`); // 재통보
    w.clock(`${date}T23:59:00`);

    const times = (d: string) => w.sentTo(d).map(([t]) => t).sort();
    eq(times(A), [`${date}T07:30:00`, `${date}T09:30:00`, `${date}T11:30:00`, `${date}T13:30:00`], "pushes received by M8101");
    eq(times(B), [`${date}T13:35:00`, `${date}T15:15:00`], "pushes received by M8102");
    eq(times(C), [`${date}T08:10:00`], "pushes received by M8103");

    const retryTexts = w
      .sentTo(A)
      .filter(([, m]) => m.includes("다시 출금"))
      .map(([t, m]) => `${t.slice(11, 16)} ${/12\/3 (\d\d:\d\d)/.exec(m)?.[1]}`)
      .sort();
    eq(retryTexts, ["09:30 10:00", "11:30 12:00", "13:30 14:01"], "M8101 retry notices name the retry they announce");

    eq(w.sentAll().length, 7, "pushes the vendor actually sent");
    eq(w.queued(), [], "nothing left in the vendor queue");
    const r = w.report(date);
    eq([r.planned, r.delivered, r.missing], [7, 7, 0], `report counts (${r.text})`);
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
console.log(failed === 0 ? "decision: all passed" : `decision: ${failed} failed`);
process.exitCode = failed === 0 ? 0 : 1;
