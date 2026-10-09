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

function sec(t: string): number {
  const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})$/.exec(t);
  if (!m) throw new Error(`bad time ${t}`);
  const [y, mo, d, h, mi, s] = m.slice(1).map(Number);
  return Date.UTC(y, mo - 1, d, h, mi, s) / 1000;
}

function fmt(s: number): string {
  return new Date(s * 1000).toISOString().slice(0, 19);
}

/** 고객이 설정한 발송 시점(출금 − lead, 야간 22:00~07:00 이면 직전 저녁 21:00) */
function nominal(due: string, lead: number): string {
  const t = fmt(sec(due) - lead * 60);
  const hm = t.slice(11, 16);
  if (hm >= "22:00") return `${t.slice(0, 10)}T21:00:00`;
  if (hm < "07:00") return `${fmt(sec(t) - 86400).slice(0, 10)}T21:00:00`;
  return t;
}

interface Expect { member: string; latest: string }

function world() {
  const home = mkdtempSync(join(tmpdir(), "autopay-h-"));
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
  let n = 0;
  const member = (lead: number): { id: string; device: string } => {
    n++;
    const id = `M${7000 + n}`;
    const device = `${n % 2 ? "ios" : "and"}:Hd${String(n).padStart(4, "0")}xQ7rT`;
    call("POST", "/members", { id, name: `고객${n}`, device, leadMinutes: lead });
    return { id, device };
  };
  const autopay = (memberId: string, merchant: string, day: number, time: string): string =>
    (call("POST", "/autopays", { memberId, merchant, amount: 10000 + n * 1000, billingDay: day, billingTime: time }) as { id: string }).id;
  return {
    member,
    autopay,
    plan: (date: string) => call("POST", "/reminders/plan", { date }),
    clock: (until: string) => call("POST", "/clock", { until }),
    fail: (id: string, failedAt: string) => call("POST", `/autopays/${id}/failures`, { failedAt }),
    report: (date: string) => call("GET", `/reports/reminders/${date}`) as { planned: number; delivered: number; missing: number },
    /** 벤더가 실제로 내보낸 푸시(기기별 시각) */
    sentTo: (device: string): string[] => vendor(["log", "--to", device]).map((l) => l.split("\t")[0]),
    sentAll: (): string[] => vendor(["log"]),
    queued: (): string[] => vendor(["list"]),
  };
}

type W = ReturnType<typeof world>;

/**
 * 각 고객이 asOf 까지 받은 푸시가 정확히 기대한 것인지 — 설정 시점(latest)보다 늦지 않고,
 * 그 직전 5분 안(예약 가능한 가장 가까운 시각)에 도착해야 한다.
 */
function checkInboxes(w: W, devices: Map<string, string>, expects: Expect[], asOf: string, label: string): void {
  for (const [member, device] of devices) {
    const want = expects
      .filter((e) => e.member === member)
      .map((e) => fmt(sec(e.latest) - (sec(e.latest) % 300)))
      .filter((t) => sec(t) <= sec(asOf))
      .sort();
    const got = w.sentTo(device).sort();
    eq(got, want, `${label}: pushes received by ${member}`);
  }
}

const cases: [string, () => void][] = [
  ["a whole billing day with odd merchant times and retries reaches everyone in time", () => {
    const w = world();
    const date = "2026-12-07";
    const times = ["08:00", "08:07", "09:13", "09:59", "10:20", "11:11", "12:34", "13:01", "14:45", "15:52", "16:16", "17:29", "18:05", "19:48", "20:02", "21:37", "07:04"];
    const leads = [10, 30, 60, 1440];
    const devices = new Map<string, string>();
    const expects: Expect[] = [];
    const contracts: string[] = [];
    times.forEach((t, i) => {
      const lead = i === times.length - 1 ? 10 : leads[i % 4];
      const m = w.member(lead);
      devices.set(m.id, m.device);
      contracts.push(w.autopay(m.id, `가맹점${i + 1}`, 7, t));
      expects.push({ member: m.id, latest: nominal(`${date}T${t}:00`, lead) });
    });
    const memberOf = [...devices.keys()];
    w.plan(date);

    const failures: [number, string][] = [[2, "2026-12-07T09:13:05"], [6, "2026-12-07T12:34:59"], [14, "2026-12-07T20:40:00"]];
    const checkpoints = ["2026-12-07T09:13:05", "2026-12-07T12:00:00", "2026-12-07T12:34:59", "2026-12-07T20:40:00", "2026-12-07T23:59:59"];
    for (const cp of checkpoints) {
      w.clock(cp);
      for (const [idx, at] of failures) {
        if (at !== cp) continue;
        w.fail(contracts[idx], at);
        expects.push({ member: memberOf[idx], latest: nominal(fmt(sec(at) + 120 * 60), 30) });
      }
      checkInboxes(w, devices, expects, cp, `as of ${cp}`);
    }
    const r = w.report(date);
    eq([r.planned, r.delivered, r.missing], [20, 20, 0], "report counts");
    eq(w.sentAll().length, 20, "pushes the vendor actually sent");
    eq(w.queued(), [], "nothing left in the vendor queue");
  }],
  ["the original billing-day sample delivers every reminder no later than promised", () => {
    const w = world();
    const devices = new Map<string, string>();
    const expects: Expect[] = [];
    const plan: [number, string][] = [[30, "08:00"], [60, "10:20"], [30, "13:47"], [10, "18:33"], [1440, "09:00"]];
    const ids: string[] = [];
    for (const [lead, t] of plan) {
      const m = w.member(lead);
      devices.set(m.id, m.device);
      ids.push(w.autopay(m.id, "든든손해보험", 12, t));
      expects.push({ member: m.id, latest: nominal(`2026-10-12T${t}:00`, lead) });
    }
    w.plan("2026-10-12");
    w.clock("2026-10-12T08:00:37");
    w.fail(ids[0], "2026-10-12T08:00:37");
    expects.push({ member: [...devices.keys()][0], latest: "2026-10-12T09:30:37" });
    w.clock("2026-10-12T23:59:00");
    checkInboxes(w, devices, expects, "2026-10-12T23:59:00", "sample");
    eq(w.sentAll().length, 6, "vendor sent");
    eq(w.queued(), [], "vendor queue empty");
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
