export type Value = number | string | boolean | Value[];

export interface Entry {
  key: string;
  value: Value;
}

function writeNumber(n: number): string {
  if (n === Number.NaN) return "Float.NaN";
  if (n === Number.POSITIVE_INFINITY) return "Float.POSITIVE_INFINITY";
  if (n === Number.NEGATIVE_INFINITY) return "Float.NEGATIVE_INFINITY";
  return String(n);
}

export function writeValue(v: Value): string {
  if (typeof v === "number") return writeNumber(v);
  if (typeof v === "string") return JSON.stringify(v);
  if (typeof v === "boolean") return v ? "true" : "false";
  return `[${v.map(writeValue).join(", ")}]`;
}

/** Serializes entries into the `.cfg` text format read by `readConfig`. */
export function writeConfig(entries: Entry[]): string {
  return entries.map((e) => `${e.key} = ${writeValue(e.value)}`).join("\n") + "\n";
}
