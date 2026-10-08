import { HttpClient, OrderService } from "./orderService";

declare const process: { exitCode?: number };

type Case = [string, () => Promise<void>];

function assertEqual(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

function httpOk(id: string): HttpClient {
  return { post: async () => ({ id }) };
}

function httpDown(status: string): HttpClient {
  return { post: async () => { throw new Error(status); } };
}

const order = { customerId: "c-1", lines: [{ sku: "A-100", qty: 2 }] };

const cases: Case[] = [
  ["submit succeeds", async () => {
    const svc = new OrderService(httpOk("o-42"));
    assertEqual(await svc.submit(order), { ok: true, id: "o-42" }, "result");
    assertEqual(svc.failures, [], "failures");
  }],
  ["invalid order is reported as a failure", async () => {
    const svc = new OrderService(httpOk("o-1"));
    const res = await svc.submit({ customerId: "c-1", lines: [{ sku: "A-100", qty: 0 }] });
    assertEqual(res, { ok: false, error: "invalid qty for A-100" }, "result");
    assertEqual(svc.failures, ["submit: invalid qty for A-100"], "failures");
  }],
  ["server error is reported as a failure", async () => {
    const svc = new OrderService(httpDown("503 Service Unavailable"));
    const res = await svc.submit(order);
    assertEqual(res, { ok: false, error: "503 Service Unavailable" }, "result");
    assertEqual(svc.failures, ["submit: 503 Service Unavailable"], "failures");
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
