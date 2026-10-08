import { Entry, Value } from "./writer";

const CONSTANTS: Record<string, number> = {
  "Float.NaN": Number.NaN,
  "Float.POSITIVE_INFINITY": Number.POSITIVE_INFINITY,
  "Float.NEGATIVE_INFINITY": Number.NEGATIVE_INFINITY,
};

const TOKEN = /\s*(\[|\]|,|"(?:[^"\\]|\\.)*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?|[A-Za-z_][A-Za-z0-9_.]*)/y;

function tokenize(src: string, line: number): string[] {
  const out: string[] = [];
  TOKEN.lastIndex = 0;
  while (TOKEN.lastIndex < src.length) {
    if (/^\s*$/.test(src.slice(TOKEN.lastIndex))) break;
    const m = TOKEN.exec(src);
    if (!m) throw new Error(`line ${line}: cannot read '${src.slice(TOKEN.lastIndex).trim()}'`);
    out.push(m[1]);
  }
  return out;
}

function parseValue(tokens: string[], pos: { i: number }, line: number): Value {
  const t = tokens[pos.i++];
  if (t === undefined) throw new Error(`line ${line}: missing value`);
  if (t === "[") {
    const items: Value[] = [];
    if (tokens[pos.i] === "]") { pos.i++; return items; }
    for (;;) {
      items.push(parseValue(tokens, pos, line));
      const sep = tokens[pos.i++];
      if (sep === "]") return items;
      if (sep !== ",") throw new Error(`line ${line}: expected ',' or ']'`);
    }
  }
  if (t.startsWith('"')) return JSON.parse(t) as string;
  if (t === "true" || t === "false") return t === "true";
  if (/^-?\d/.test(t)) return Number(t);
  if (t in CONSTANTS) return CONSTANTS[t];
  throw new Error(`line ${line}: unexpected token '${t}'`);
}

/** Reads the `.cfg` text format. Strict: anything outside the grammar is rejected. */
export function readConfig(text: string): Entry[] {
  const entries: Entry[] = [];
  text.split("\n").forEach((raw, idx) => {
    const line = idx + 1;
    if (raw.trim() === "" || raw.trim().startsWith("#")) return;
    const eq = raw.indexOf("=");
    if (eq < 0) throw new Error(`line ${line}: expected 'key = value'`);
    const key = raw.slice(0, eq).trim();
    const tokens = tokenize(raw.slice(eq + 1), line);
    const pos = { i: 0 };
    const value = parseValue(tokens, pos, line);
    if (pos.i !== tokens.length) throw new Error(`line ${line}: trailing input`);
    entries.push({ key, value });
  });
  return entries;
}
