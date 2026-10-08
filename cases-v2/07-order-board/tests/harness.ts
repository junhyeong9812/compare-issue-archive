declare const process: { exitCode?: number };

type Fn = () => void | Promise<void>;
const registered: [string, Fn][] = [];

export function test(name: string, fn: Fn): void {
  registered.push([name, fn]);
}

export function eq(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

export async function run(): Promise<void> {
  let failed = 0;
  for (const [name, fn] of registered) {
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
}

export class FakeClock {
  constructor(public t = 1_700_000_000_000) {}
  now(): number { return this.t; }
  advance(ms: number): void { this.t += ms; }
}
