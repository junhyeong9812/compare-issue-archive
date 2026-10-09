/**
 * 서비스 현지 시각(KST) 문자열 "YYYY-MM-DDTHH:MM:SS".
 * 오프셋을 붙이지 않는다 — 서비스 전체가 한 시간대만 쓴다.
 * 계산은 Date.UTC 로 해서 실행 머신의 TZ 설정과 무관하게 만든다.
 */
export type LocalDateTime = string;
export type LocalDate = string;

const RE = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})$/;

function pad(n: number, w = 2): string {
  return String(n).padStart(w, "0");
}

export function toEpochSeconds(t: LocalDateTime): number {
  const m = RE.exec(t);
  if (!m) throw new Error(`bad local time: ${t}`);
  const [y, mo, d, h, mi, s] = m.slice(1).map(Number);
  const ms = Date.UTC(y, mo - 1, d, h, mi, s);
  const back = new Date(ms);
  if (back.getUTCMonth() !== mo - 1 || back.getUTCDate() !== d || h > 23 || mi > 59 || s > 59) {
    throw new Error(`bad local time: ${t}`);
  }
  return ms / 1000;
}

export function fromEpochSeconds(sec: number): LocalDateTime {
  const d = new Date(sec * 1000);
  return (
    `${pad(d.getUTCFullYear(), 4)}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}` +
    `T${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}:${pad(d.getUTCSeconds())}`
  );
}

export function isLocalDateTime(t: unknown): t is LocalDateTime {
  if (typeof t !== "string" || !RE.test(t)) return false;
  try {
    toEpochSeconds(t);
    return true;
  } catch {
    return false;
  }
}

export function addMinutes(t: LocalDateTime, minutes: number): LocalDateTime {
  return fromEpochSeconds(toEpochSeconds(t) + minutes * 60);
}

export function compare(a: LocalDateTime, b: LocalDateTime): number {
  return toEpochSeconds(a) - toEpochSeconds(b);
}

export function dateOf(t: LocalDateTime): LocalDate {
  return t.slice(0, 10);
}

export function timeOf(t: LocalDateTime): string {
  return t.slice(11, 16);
}

/** "2026-10-12" + "13:47" → "2026-10-12T13:47:00" */
export function at(date: LocalDate, hhmm: string): LocalDateTime {
  const t = `${date}T${hhmm}:00`;
  toEpochSeconds(t);
  return t;
}

export function dayOfMonth(date: LocalDate): number {
  return Number(date.slice(8, 10));
}

export function previousDate(date: LocalDate): LocalDate {
  return dateOf(addMinutes(at(date, "00:00"), -1440));
}
