/** Rounds to a fixed number of decimals. Non-finite values (NaN, ±Infinity) are returned unchanged. */
export function roundTo(n: number, decimals: number): number {
  if (!Number.isFinite(n)) return n;
  const f = 10 ** decimals;
  const r = Math.round(n * f) / f;
  return Object.is(r, -0) ? 0 : r;
}

/** Display helper for the history view. */
export function formatNumber(n: number): string {
  if (Number.isNaN(n)) return "n/a";
  if (n === Number.POSITIVE_INFINITY) return "∞";
  if (n === Number.NEGATIVE_INFINITY) return "-∞";
  return String(n);
}
