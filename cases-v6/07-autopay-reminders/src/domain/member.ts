export interface Member {
  id: string;
  name: string;
  /** 앱 푸시 기기 토큰 — "ios:" 또는 "and:" 접두 */
  device: string;
  /** 출금 예정 알림을 출금 몇 분 전에 받을지 */
  leadMinutes: number;
  pushOptIn: boolean;
}

export function validateMember(input: unknown, allowedLeads: number[]): Omit<Member, "device"> & { device: string } {
  const b = (input ?? {}) as Record<string, unknown>;
  if (typeof b.id !== "string" || !/^M\d{4,}$/.test(b.id)) throw new Error("member id must look like M1234");
  if (typeof b.name !== "string" || b.name.trim() === "") throw new Error("name required");
  if (typeof b.device !== "string") throw new Error("device required");
  const lead = b.leadMinutes === undefined ? 30 : b.leadMinutes;
  if (typeof lead !== "number" || !allowedLeads.includes(lead)) throw new Error(`leadMinutes must be one of ${allowedLeads.join(",")}`);
  return {
    id: b.id,
    name: b.name.trim(),
    device: b.device,
    leadMinutes: lead,
    pushOptIn: b.pushOptIn !== false,
  };
}
