import { createConsole } from "../src/app";
import { ScriptStep, ScriptedProcess } from "../src/runtime/process";

declare const process: { exitCode?: number };

function eq(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

interface Detail { status: string; exitCode: number | null; log: string[] }

const SCRIPTS: Record<string, ScriptStep[]> = {
  "test unit": [
    { at: 50, write: "\x1b[32mrunning 40 tests\x1b[0m\n" },
    { at: 100, write: "test a ... ok\n" },
    { at: 150, write: "test b ... FAILED\n" },
    { at: 150, write: "1 failed" },
    { at: 150, exit: 2 },
  ],
  "deploy": [
    { at: 50, write: "upload 10%\rupload 55%\rupload 100%\n" },
    { at: 900, write: "switching traffic\n" },
    { at: 950, write: "done\n" },
    { at: 1000, exit: 0 },
  ],
  "lint": [
    { at: 50, write: "checking\n" },
    { at: 100, write: "clean\n" },
    { at: 100, exit: 0 },
  ],
  "long": [
    { at: 50, write: "phase 1\n" },
    { at: 120, write: "phase 2\n" },
    { at: 9000, exit: 0 },
  ],
};

function app(tickMs: number) {
  return createConsole((cmd, now) => new ScriptedProcess(SCRIPTS[cmd] ?? [], now), { tickMs });
}

function launch(a: ReturnType<typeof app>, command: string): string {
  return (a.router.handle("POST", "/jobs", { name: command, command }).body as { id: string }).id;
}

function detail(a: ReturnType<typeof app>, id: string): Detail {
  const d = a.router.handle("GET", `/jobs/${id}`).body as Detail;
  return { status: d.status, exitCode: d.exitCode, log: d.log };
}

const cases: [string, () => void][] = [
  ["failing job: full output, failed, at a different tick rate", () => {
    const a = app(50);
    const id = launch(a, "test unit");
    a.runFor(2000);
    const d = detail(a, id);
    eq(d.log, ["running 40 tests", "test a ... ok", "test b ... FAILED", "1 failed"], "log");
    eq(d.status, "failed", "status");
    eq(d.exitCode, 2, "exit");
  }],
  ["several jobs ending in the same tick all end with complete output", () => {
    const a = app(50);
    const ids = [launch(a, "test unit"), launch(a, "deploy"), launch(a, "lint")];
    a.runFor(3000);
    eq(ids.map((id) => detail(a, id).status), ["failed", "succeeded", "succeeded"], "statuses");
    eq(detail(a, ids[1]).log, ["upload 100%", "switching traffic", "done"], "deploy log");
    eq(detail(a, ids[2]).log, ["checking", "clean"], "lint log");
    eq(a.activeJobs(), [], "active");
  }],
  ["a running job shows its latest output and stays running", () => {
    const a = app(100);
    const id = launch(a, "long");
    a.runFor(2000);
    eq(detail(a, id), { status: "running", exitCode: null, log: ["phase 1", "phase 2"] }, "mid-run");
    a.runFor(8000);
    eq(detail(a, id).status, "succeeded", "after exit");
  }],
  ["re-running a command opens a new job while the old one stays finished", () => {
    const a = app(100);
    const first = launch(a, "lint");
    a.runFor(1000);
    const second = launch(a, "long");
    a.runFor(500);
    eq(detail(a, first).status, "succeeded", "first");
    eq(detail(a, second).status, "running", "second");
    eq(detail(a, second).log, ["phase 1", "phase 2"], "second log");
  }],
  ["remote runner events: a late snapshot after exit keeps the job finished", () => {
    const a = app(100);
    const r = a.router.handle("POST", "/events", [
      { kind: "started", jobId: "r-1", name: "remote", at: 10 },
      { kind: "timeline", jobId: "r-1", seq: 1, lines: ["fetch"] },
      { kind: "closed", jobId: "r-1", exitCode: 1, at: 40 },
      { kind: "timeline", jobId: "r-1", seq: 2, lines: ["fetch", "error: 404"] },
      { kind: "timeline", jobId: "r-1", seq: 1, lines: ["fetch"] },
    ]);
    eq(r.status, 202, "accepted");
    eq(detail(a, "r-1"), { status: "failed", exitCode: 1, log: ["fetch", "error: 404"] }, "view");
  }],
  ["remote runner events: exit announced before any output", () => {
    const a = app(100);
    a.router.handle("POST", "/events", { kind: "started", jobId: "r-2", name: "remote", at: 0 });
    a.router.handle("POST", "/events", { kind: "closed", jobId: "r-2", exitCode: 0, at: 30 });
    a.router.handle("POST", "/events", { kind: "timeline", jobId: "r-2", seq: 1, lines: ["a"] });
    a.router.handle("POST", "/events", { kind: "timeline", jobId: "r-2", seq: 2, lines: ["a", "b"] });
    eq(detail(a, "r-2"), { status: "succeeded", exitCode: 0, log: ["a", "b"] }, "view");
    const rows = a.router.handle("GET", "/jobs").body as { id: string; status: string }[];
    eq(rows.map((x) => x.status), ["succeeded"], "list");
  }],
];

let failed = 0;
for (const [name, fn] of cases) {
  try { fn(); console.log(`ok   - ${name}`); }
  catch (e) { failed++; console.log(`FAIL - ${name}: ${e instanceof Error ? e.message : String(e)}`); }
}
console.log(failed === 0 ? "all passed" : `${failed} failed`);
process.exitCode = failed === 0 ? 0 : 1;
