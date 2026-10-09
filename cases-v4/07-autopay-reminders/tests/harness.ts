type Fn = () => void;
const registered: [string, Fn][] = [];
const cleanups: (() => void)[] = [];

export function test(name: string, fn: Fn): void {
  registered.push([name, fn]);
}

export function onCleanup(fn: () => void): void {
  cleanups.push(fn);
}

export function eq(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

export function assert(cond: boolean, label: string): void {
  if (!cond) throw new Error(label);
}

export function run(): void {
  let failed = 0;
  for (const [name, fn] of registered) {
    try {
      fn();
      console.log(`ok   - ${name}`);
    } catch (e) {
      failed++;
      console.log(`FAIL - ${name}: ${e instanceof Error ? e.message : String(e)}`);
    }
  }
  for (const c of cleanups) {
    try {
      c();
    } catch {
      /* ignore */
    }
  }
  console.log(failed === 0 ? "all passed" : `${failed} failed`);
  process.exitCode = failed === 0 ? 0 : 1;
}
