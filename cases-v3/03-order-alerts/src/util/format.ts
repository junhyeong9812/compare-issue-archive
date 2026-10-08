/**
 * 국내 휴대전화 번호를 숫자 11자리(010xxxxxxxx)로 정규화한다.
 * +82 국가번호·하이픈·공백·괄호를 허용한다. 정규화할 수 없으면 null.
 */
export function normalizeMobile(raw: string): string | null {
  let digits = raw.replace(/[\s\-()]/g, "");
  if (digits.startsWith("+82")) digits = "0" + digits.slice(3);
  else if (digits.startsWith("82") && digits.length === 12) digits = "0" + digits.slice(2);
  if (!/^\d+$/.test(digits)) return null;
  if (!/^01[016789]\d{7,8}$/.test(digits)) return null;
  if (digits.length === 10) digits = digits.slice(0, 3) + "0" + digits.slice(3);
  return digits;
}

export function maskMobile(n: string): string {
  return n.length === 11 ? `${n.slice(0, 3)}-****-${n.slice(7)}` : "***";
}

/** 표시 폭(한글 2칸, 그 외 1칸) */
export function displayWidth(s: string): number {
  let w = 0;
  for (const ch of s) w += /[ᄀ-ᇿ㄰-㆏가-힣]/.test(ch) ? 2 : 1;
  return w;
}

export function padEnd(s: string, width: number): string {
  const gap = width - displayWidth(s);
  return gap > 0 ? s + " ".repeat(gap) : s;
}

/** 문자 본문 길이 제한(표시 폭 기준) — 넘치면 말줄임 */
export function clip(s: string, width: number): string {
  if (displayWidth(s) <= width) return s;
  let out = "";
  for (const ch of s) {
    if (displayWidth(out + ch) > width - 1) break;
    out += ch;
  }
  return out + "…";
}
