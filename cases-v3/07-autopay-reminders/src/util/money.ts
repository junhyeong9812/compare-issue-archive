/** 12345 → "12,345원" */
export function won(amount: number): string {
  const s = String(Math.abs(Math.trunc(amount)));
  let out = "";
  for (let i = 0; i < s.length; i++) {
    if (i > 0 && (s.length - i) % 3 === 0) out += ",";
    out += s[i];
  }
  return `${amount < 0 ? "-" : ""}${out}원`;
}

/** 가맹점명을 알림 한 줄에 맞게 줄인다(한글 기준 12자). */
export function shortMerchant(name: string, max = 12): string {
  const chars = [...name];
  return chars.length <= max ? name : `${chars.slice(0, max - 1).join("")}…`;
}
