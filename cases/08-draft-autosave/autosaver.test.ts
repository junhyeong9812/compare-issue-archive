import { DraftAutosaver } from "./autosaver";

declare const process: { exitCode?: number };

class FakeClock {
  t = 0;
  now(): number { return this.t; }
}

class FakeStore {
  writes: { at: number; text: string }[] = [];
  attempts = 0;
  failNext = 0;
  constructor(private readonly clock: FakeClock) {}
  write(text: string): void {
    this.attempts++;
    if (this.failNext > 0) { this.failNext--; throw new Error("EIO: disk busy"); }
    this.writes.push({ at: this.clock.t, text });
  }
  last(): string | undefined { return this.writes[this.writes.length - 1]?.text; }
}

function assertEqual(actual: unknown, expected: unknown, label: string): void {
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) throw new Error(`${label}: expected ${e}, got ${a}`);
}

/** Simulates the host loop: one tick every `step` ms; `edits` maps time -> new buffer content. */
function run(saver: DraftAutosaver, clock: FakeClock, buffer: { text: string }, edits: Map<number, string>, until: number, step = 100): void {
  for (; clock.t <= until; clock.t += step) {
    const e = edits.get(clock.t);
    if (e !== undefined) buffer.text = e;
    saver.tick();
  }
}

const cases: [string, () => void][] = [
  ["the last edit is saved after the user stops typing", () => {
    const clock = new FakeClock();
    const store = new FakeStore(clock);
    const buffer = { text: "" };
    const saver = new DraftAutosaver(() => buffer.text, store, clock, 1000);
    const live: string[] = [];
    saver.onChange((t) => live.push(t));
    run(saver, clock, buffer, new Map([[0, "H"], [200, "He"], [400, "Hel"], [600, "Hell"], [800, "Hello"]]), 5000);
    assertEqual(live, ["H", "He", "Hel", "Hell", "Hello"], "live preview");
    assertEqual(store.last(), "Hello", "last saved draft");
  }],
  ["saves are spaced by the interval while typing", () => {
    const clock = new FakeClock();
    const store = new FakeStore(clock);
    const buffer = { text: "" };
    const saver = new DraftAutosaver(() => buffer.text, store, clock, 1000);
    const edits = new Map<number, string>();
    for (let t = 0; t <= 2500; t += 100) edits.set(t, "x".repeat(t / 100 + 1));
    run(saver, clock, buffer, edits, 2500);
    const times = store.writes.map((w) => w.at);
    for (let i = 1; i < times.length; i++) {
      if (times[i] - times[i - 1] < 1000) throw new Error(`writes too close: ${times}`);
    }
    if (times.length < 2) throw new Error(`expected periodic saves while typing, got ${times}`);
  }],
  ["a failed save is retried later", () => {
    const clock = new FakeClock();
    const store = new FakeStore(clock);
    store.failNext = 1;
    const buffer = { text: "" };
    const saver = new DraftAutosaver(() => buffer.text, store, clock, 1000);
    run(saver, clock, buffer, new Map([[0, "draft v1"]]), 4000);
    assertEqual(store.last(), "draft v1", "last saved draft");
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
