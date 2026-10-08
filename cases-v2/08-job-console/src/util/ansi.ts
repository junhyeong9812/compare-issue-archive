// CSI sequences (colors, cursor moves) and OSC sequences (titles, hyperlinks).
const CSI = /\x1b\[[0-9;?]*[ -/]*[@-~]/g;
const OSC = /\x1b\][^\x07\x1b]*(?:\x07|\x1b\\)/g;

export function stripAnsi(s: string): string {
  return s.replace(OSC, "").replace(CSI, "");
}

/** Applies carriage returns the way a terminal would: text after the last \r wins. */
export function collapseCarriageReturns(line: string): string {
  const i = line.lastIndexOf("\r");
  return i < 0 ? line : line.slice(i + 1);
}
