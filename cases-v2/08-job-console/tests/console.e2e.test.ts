import { createConsole } from "../src/app";
import { ScriptStep, ScriptedProcess } from "../src/runtime/process";
import { eq, run, test } from "./harness";

const SCRIPTS: Record<string, ScriptStep[]> = {
  "make build": [
    { at: 100, write: "compiling 12 files\n" },
    { at: 200, write: "linking\n" },
    { at: 300, write: "build ok\n" },
    { at: 300, exit: 0 },
  ],
  "make slow": [
    { at: 100, write: "waiting for db\n" },
    { at: 1500, write: "db ready\n" },
    { at: 2600, exit: 0 },
  ],
};

function consoleWithScripts() {
  return createConsole((cmd, now) => new ScriptedProcess(SCRIPTS[cmd] ?? [], now));
}

interface Detail { status: string; exitCode: number | null; log: string[] }

test("job page shows the whole output once a quick job finishes", () => {
  const app = consoleWithScripts();
  const id = (app.router.handle("POST", "/jobs", { name: "build", command: "make build" }).body as { id: string }).id;
  app.runFor(3000);
  const d = app.router.handle("GET", `/jobs/${id}`).body as Detail;
  eq(d.log, ["compiling 12 files", "linking", "build ok"], "log");
  eq(d.status, "succeeded", "status");
  eq(d.exitCode, 0, "exit code");
});

test("a job that went quiet before exiting shows all output and finishes", () => {
  const app = consoleWithScripts();
  const id = (app.router.handle("POST", "/jobs", { name: "slow", command: "make slow" }).body as { id: string }).id;
  app.runFor(5000);
  const d = app.router.handle("GET", `/jobs/${id}`).body as Detail;
  eq(d.log, ["waiting for db", "db ready"], "log");
  eq(d.status, "succeeded", "status");
});

test("finished jobs stay finished in the list", () => {
  const app = consoleWithScripts();
  app.router.handle("POST", "/jobs", { name: "build", command: "make build" });
  app.router.handle("POST", "/jobs", { name: "slow", command: "make slow" });
  app.runFor(5000);
  const rows = app.router.handle("GET", "/jobs").body as { status: string }[];
  eq(rows.map((r) => r.status), ["succeeded", "succeeded"], "statuses");
  eq(app.activeJobs(), [], "nothing left running");
});

run();
