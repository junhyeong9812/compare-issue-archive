import { HttpClient, OrderService } from "./orderService";

declare const process: { exitCode?: number };

type Case = [string, () => Promise<void>];

function assertEqual(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

function httpLater(outcome: { id?: string; reject?: unknown }, ms: number): HttpClient & { calls: string[] } {
  const calls: string[] = [];
  return {
    calls,
    post: (path: string) => {
      calls.push(path);
      return new Promise((resolve, reject) =>
        setTimeout(() => (outcome.reject !== undefined ? reject(outcome.reject) : resolve({ id: outcome.id ?? "x" })), ms));
    },
  };
}

const cases: Case[] = [
  ["cancel: delayed server error becomes a failure result", async () => {
    const http = httpLater({ reject: new Error("502 Bad Gateway") }, 5);
    const svc = new OrderService(http);
    assertEqual(await svc.cancel("o-7"), { ok: false, error: "502 Bad Gateway" }, "result");
    assertEqual(svc.failures, ["cancel: 502 Bad Gateway"], "failures");
    assertEqual(http.calls, ["/orders/o-7/cancel"], "calls");
  }],
  ["cancel: success still returns the id", async () => {
    const svc = new OrderService(httpLater({ id: "ignored" }, 1));
    assertEqual(await svc.cancel("o-9"), { ok: true, id: "o-9" }, "result");
    assertEqual(svc.failures, [], "failures");
  }],
  ["cancel: missing id is a failure", async () => {
    const svc = new OrderService(httpLater({ id: "x" }, 1));
    assertEqual(await svc.cancel(""), { ok: false, error: "orderId is required" }, "result");
  }],
  ["submit: non-Error rejection after a delay", async () => {
    const svc = new OrderService(httpLater({ reject: "timeout after 30s" }, 3));
    const res = await svc.submit({ customerId: "c-9", lines: [{ sku: "B-2", qty: 1 }, { sku: "C-3", qty: 4 }] });
    assertEqual(res, { ok: false, error: "timeout after 30s" }, "result");
    assertEqual(svc.failures, ["submit: timeout after 30s"], "failures");
  }],
  ["submit then cancel both fail and are both recorded in order", async () => {
    const svc = new OrderService(httpLater({ reject: new Error("connection reset") }, 2));
    await svc.submit({ customerId: "c-2", lines: [{ sku: "D-1", qty: 3 }] });
    await svc.cancel("o-1");
    assertEqual(svc.failures, ["submit: connection reset", "cancel: connection reset"], "failures");
  }],
  ["submit: delayed success", async () => {
    const svc = new OrderService(httpLater({ id: "o-100" }, 4));
    assertEqual(await svc.submit({ customerId: "c-3", lines: [{ sku: "E-5", qty: 1 }] }), { ok: true, id: "o-100" }, "result");
  }],
];

(async () => {
  let failed = 0;
  for (const [name, fn] of cases) {
    try {
      await fn();
      console.log(`ok   - ${name}`);
    } catch (e) {
      failed++;
      console.log(`FAIL - ${name}: ${e instanceof Error ? e.message : String(e)}`);
    }
  }
  console.log(failed === 0 ? "all passed" : `${failed} failed`);
  process.exitCode = failed === 0 ? 0 : 1;
})();
