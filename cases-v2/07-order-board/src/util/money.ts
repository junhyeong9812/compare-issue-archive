/** Tiny unbounded memoizer for pure single-argument functions. */
function memo<A extends string | number, R>(fn: (a: A) => R): (a: A) => R {
  const cache = new Map<A, R>();
  return (a: A) => {
    if (cache.has(a)) return cache.get(a) as R;
    const r = fn(a);
    cache.set(a, r);
    return r;
  };
}

const groupSeparator = memo((locale: string) => (locale.startsWith("de") ? "." : ","));

/** Formats minor units (no decimals) with thousands separators: 1234567 -> "1,234,567원". */
export function formatWon(amount: number, locale = "ko-KR"): string {
  const sign = amount < 0 ? "-" : "";
  const digits = String(Math.abs(Math.trunc(amount)));
  const sep = groupSeparator(locale);
  let out = "";
  for (let i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 === 0) out += sep;
    out += digits[i];
  }
  return `${sign}${out}원`;
}
