import { readConfig } from "./reader";
import { Entry, writeConfig } from "./writer";

declare const process: { exitCode?: number };

function same(a: unknown, b: unknown): boolean {
  if (typeof a === "number" && typeof b === "number") return Object.is(a, b) || a === b;
  if (Array.isArray(a) && Array.isArray(b)) return a.length === b.length && a.every((x, i) => same(x, b[i]));
  return a === b;
}

function roundTrip(entries: Entry[]): void {
  const text = writeConfig(entries);
  let back: Entry[];
  try {
    back = readConfig(text);
  } catch (e) {
    throw new Error(`exported file could not be read back (${e instanceof Error ? e.message : e}):\n${text}`);
  }
  if (back.length !== entries.length) throw new Error(`entry count ${back.length} != ${entries.length}`);
  entries.forEach((e, i) => {
    if (back[i].key !== e.key || !same(back[i].value, e.value)) {
      throw new Error(`entry ${e.key} changed: ${String(e.value)} -> ${String(back[i].value)}`);
    }
  });
}

const sensorReadings = [12.5, 0.1 + 0.2, 1e21];
const failedReading = 0 / 0;

const cases: [string, () => void][] = [
  ["plain settings survive export/import", () => roundTrip([
    { key: "name", value: "line-3 \"west\"" },
    { key: "enabled", value: true },
    { key: "readings", value: sensorReadings },
    { key: "retries", value: -4 },
  ])],
  ["open-ended limits survive export/import", () => roundTrip([
    { key: "max_temp", value: Number.POSITIVE_INFINITY },
    { key: "min_temp", value: Number.NEGATIVE_INFINITY },
  ])],
  ["calibration with a failed reading survives export/import", () => roundTrip([
    { key: "sensor", value: "probe-a" },
    { key: "offset", value: failedReading },
    { key: "scale", value: 1.02 },
  ])],
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
