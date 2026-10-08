import { createApp } from "../src/app";

declare const process: { exitCode?: number };

function eq(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}
function truthy(c: boolean, label: string): void { if (!c) throw new Error(label); }

let t = 1_760_000_000_000;
const clock = { now: () => (t += 500) };

type S = Record<string, unknown>;
const prof = (id: string, settings: S, firmware = "3.4.1") => ({ deviceId: id, name: `dev ${id}`, firmware, settings });
const report = (app: ReturnType<typeof createApp>, id: string, settings: S) =>
  app.handle("POST", `/devices/${id}/report`, prof(id, settings)).body as { rev: number; changed: boolean };
const history = (app: ReturnType<typeof createApp>, id: string) =>
  app.handle("GET", `/devices/${id}/revisions`).body as { count: number; items: { rev: number; changed: string[]; settings: S; summary: string }[] };

const cases: [string, () => void][] = [
  ["undefined readings from different sources repeat without new revisions", () => {
    const app = createApp(clock);
    const sources = [Number.NaN, Math.sqrt(-1), 0 / 0, Number("n/a"), Number.POSITIVE_INFINITY - Number.POSITIVE_INFINITY];
    sources.forEach((v, i) => {
      const r = report(app, "chiller-2", { setpoint: 6.5, curve: [1, v, 3], meta: { cal: { slope: v, at: "2026-01-01" } } });
      eq(r, { rev: 1, changed: i === 0 }, `report ${i}`);
    });
    eq(history(app, "chiller-2").count, 1, "count");
  }],
  ["switching between non-finite values and null are real changes", () => {
    const app = createApp(clock);
    eq(report(app, "fan-1", { limit: Number.POSITIVE_INFINITY }).rev, 1, "inf");
    eq(report(app, "fan-1", { limit: Number.NaN }), { rev: 2, changed: true }, "inf -> undefined reading");
    eq(report(app, "fan-1", { limit: Number.NaN }), { rev: 2, changed: false }, "same again");
    eq(report(app, "fan-1", { limit: null }), { rev: 3, changed: true }, "-> null");
    eq(report(app, "fan-1", { limit: Number.NaN }), { rev: 4, changed: true }, "null -> undefined reading");
    eq(report(app, "fan-1", { limit: 0 }), { rev: 5, changed: true }, "-> 0");
    eq(report(app, "fan-1", { limit: Number.NEGATIVE_INFINITY }), { rev: 6, changed: true }, "-> -inf");
    const h = history(app, "fan-1");
    eq(h.items.map((i) => i.changed), [["limit"], ["limit"], ["limit"], ["limit"], ["limit"], ["limit"]], "changed lists");
    truthy(Number.isNaN(h.items[2].settings.limit as number), "rev 4 keeps the undefined reading");
    eq(h.items[2].summary, "limit=n/a", "summary");
    eq(h.items[3].settings.limit, null, "rev 3 is null");
  }],
  ["only the changed key is listed when another key holds an undefined reading", () => {
    const app = createApp(clock);
    report(app, "pump-9", { offset: Number.NaN, rpm: 1200 });
    eq(report(app, "pump-9", { offset: Number.NaN, rpm: 1300 }), { rev: 2, changed: true }, "rpm change");
    eq(history(app, "pump-9").items[0].changed, ["rpm"], "changed");
  }],
  ["fleet import is idempotent for devices with undefined readings", () => {
    const app = createApp(clock);
    const fleet = [
      prof("a-1", { bias: Number.NaN, mode: "auto" }),
      prof("a-2", { bias: 0.25, mode: "auto" }),
      prof("a-3", { sensors: [{ id: "x", offset: Number.NaN }] }),
    ];
    eq((app.handle("POST", "/import", fleet).body as { created: string[] }).created, ["a-1", "a-2", "a-3"], "first import");
    const again = app.handle("POST", "/import", fleet).body as { created: string[]; unchanged: string[] };
    eq([again.created, again.unchanged], [[], ["a-1", "a-2", "a-3"]], "second import");
  }],
  ["status compares requested and reported values including undefined readings", () => {
    const app = createApp(clock);
    const want = { sensors: [{ id: "x", offset: Number.NaN, gain: 1.5 }], mode: "eco" };
    app.handle("PUT", "/devices/b-1/desired", prof("b-1", want));
    report(app, "b-1", { sensors: [{ id: "x", offset: Number.NaN, gain: 1.5 }], mode: "eco", uptimeSec: 42 });
    let s = app.handle("GET", "/devices/b-1/status").body as { state: string; drift: string[] };
    eq([s.state, s.drift], ["in-sync", []], "same");
    report(app, "b-1", { sensors: [{ id: "x", offset: 0.5, gain: 1.5 }], mode: "eco" });
    s = app.handle("GET", "/devices/b-1/status").body as { state: string; drift: string[] };
    eq([s.state, s.drift], ["drifted", ["sensors"]], "different");
  }],
];

let failed = 0;
for (const [name, fn] of cases) {
  try { fn(); console.log(`ok   - ${name}`); }
  catch (e) { failed++; console.log(`FAIL - ${name}: ${e instanceof Error ? e.message : String(e)}`); }
}
console.log(failed === 0 ? "all passed" : `${failed} failed`);
process.exitCode = failed === 0 ? 0 : 1;
