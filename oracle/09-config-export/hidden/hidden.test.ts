import { readConfig } from "./reader";
import { Entry, writeConfig, writeValue } from "./writer";

declare const process: { exitCode?: number };

function same(a: unknown, b: unknown): boolean {
  if (typeof a === "number" && typeof b === "number") return Object.is(a, b) || a === b;
  if (Array.isArray(a) && Array.isArray(b)) return a.length === b.length && a.every((x, i) => same(x, b[i]));
  return a === b;
}

function assertEqual(actual: unknown, expected: unknown, label: string): void {
  if (actual !== expected) throw new Error(`${label}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
}

function roundTrip(entries: Entry[]): void {
  const back = readConfig(writeConfig(entries));
  if (back.length !== entries.length) throw new Error("entry count changed");
  entries.forEach((e, i) => {
    if (back[i].key !== e.key || !same(back[i].value, e.value)) throw new Error(`entry ${e.key} changed`);
  });
}

const cases: [string, () => void][] = [
  ["undefined results from various sources are written as the constant", () => {
    assertEqual(writeValue(Math.sqrt(-1)), "Float.NaN", "sqrt(-1)");
    assertEqual(writeValue(parseFloat("n/a")), "Float.NaN", "parseFloat");
    assertEqual(writeValue(Number.POSITIVE_INFINITY - Number.POSITIVE_INFINITY), "Float.NaN", "inf-inf");
    assertEqual(writeValue(Number.NaN), "Float.NaN", "Number.NaN");
  }],
  ["infinities and finite numbers keep their form", () => {
    assertEqual(writeValue(1 / 0), "Float.POSITIVE_INFINITY", "+inf");
    assertEqual(writeValue(-1 / 0), "Float.NEGATIVE_INFINITY", "-inf");
    assertEqual(writeValue(3.25), "3.25", "finite");
    assertEqual(writeValue(-7), "-7", "negative");
    assertEqual(writeValue(2.5e-8), "2.5e-8", "small");
  }],
  ["nested lists with special values round-trip", () => roundTrip([
    { key: "matrix", value: [[1, Math.sqrt(-2)], [Number.POSITIVE_INFINITY, 0 / 0, -1 / 0], []] },
    { key: "label", value: "NaN" },
  ])],
  ["exact file text for a mixed config", () => {
    const text = writeConfig([{ key: "gain", value: parseFloat("") }, { key: "ok", value: false }, { key: "xs", value: [0.5, Number.NaN] }]);
    assertEqual(text, "gain = Float.NaN\nok = false\nxs = [0.5, Float.NaN]\n", "text");
  }],
  ["many entries, every special position round-trips", () => {
    const entries: Entry[] = [];
    for (let i = 0; i < 20; i++) entries.push({ key: `k${i}`, value: i % 3 === 0 ? 0 / 0 : i % 3 === 1 ? i / 7 : -i / 0 });
    roundTrip(entries);
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
