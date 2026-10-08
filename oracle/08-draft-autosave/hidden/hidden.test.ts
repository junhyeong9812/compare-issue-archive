import { DraftAutosaver } from "./autosaver";

declare const process: { exitCode?: number };

class FakeClock {
  t = 0;
  now(): number { return this.t; }
}

class FakeStore {
  writes: { at: number; text: string }[] = [];
  attemptsAt: number[] = [];
  failNext = 0;
  constructor(private readonly clock: FakeClock) {}
  write(text: string): void {
    this.attemptsAt.push(this.clock.t);
    if (this.failNext > 0) { this.failNext--; throw new Error("ENOSPC"); }
    this.writes.push({ at: this.clock.t, text });
  }
  last(): string | undefined { return this.writes[this.writes.length - 1]?.text; }
}

function assertEqual(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

function spaced(times: number[], min: number, label: string): void {
  for (let i = 1; i < times.length; i++) {
    if (times[i] - times[i - 1] < min) throw new Error(`${label}: too close ${times}`);
  }
}

function setup(interval: number) {
  const clock = new FakeClock();
  const store = new FakeStore(clock);
  const buffer = { text: "" };
  const saver = new DraftAutosaver(() => buffer.text, store, clock, interval);
  const run = (edits: Map<number, string>, from: number, until: number, step: number) => {
    for (clock.t = from; clock.t <= until; clock.t += step) {
      const e = edits.get(clock.t);
      if (e !== undefined) buffer.text = e;
      saver.tick();
    }
  };
  return { clock, store, buffer, saver, run };
}

const cases: [string, () => void][] = [
  ["each burst's final text lands, with a 500ms interval and 50ms ticks", () => {
    const { store, run } = setup(500);
    const edits = new Map<number, string>([[0, "a"], [50, "ab"], [100, "abc"], [2000, "abcd"], [2050, "abcde"], [2100, "abcdef"]]);
    run(edits, 0, 1900, 50);
    assertEqual(store.last(), "abc", "after first burst");
    run(edits, 2000, 4000, 50);
    assertEqual(store.last(), "abcdef", "after second burst");
    spaced(store.writes.map((w) => w.at), 500, "writes");
  }],
  ["single change inside the interval after a save still lands", () => {
    const { store, run } = setup(300);
    run(new Map([[0, "x"], [10, "y"]]), 0, 1000, 10);
    assertEqual(store.writes.map((w) => w.text), ["x", "y"], "writes");
  }],
  ["no extra writes once idle and saved", () => {
    const { store, run } = setup(200);
    run(new Map([[0, "q"], [20, "qq"]]), 0, 5000, 20);
    assertEqual(store.writes.map((w) => w.text), ["q", "qq"], "writes");
  }],
  ["repeated failures are retried at the interval, not every tick, until success", () => {
    const { store, run } = setup(400);
    store.failNext = 3;
    run(new Map([[0, "note"], [40, "note!"]]), 0, 4000, 20);
    assertEqual(store.last(), "note!", "final");
    spaced(store.attemptsAt, 400, "attempts");
    if (store.attemptsAt.length > 6) throw new Error(`too many attempts: ${store.attemptsAt}`);
  }],
  ["failure then newer edit: newest text wins", () => {
    const { store, run } = setup(1000);
    store.failNext = 1;
    run(new Map([[0, "v1"], [500, "v2"]]), 0, 6000, 100);
    assertEqual(store.last(), "v2", "final");
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
console.log(failed === 0 ? "all passed" : `${failed} failed`);
process.exitCode = failed === 0 ? 0 : 1;
