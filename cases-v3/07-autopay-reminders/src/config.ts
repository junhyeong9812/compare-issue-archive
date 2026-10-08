import { join } from "path";

export interface AppConfig {
  /** pushq 상태 디렉토리(PUSHQ_HOME) */
  pushqHome: string;
  /** pushq 바이너리 경로 */
  pushqBin: string;
  /** 출금 실패 후 재출금까지 걸리는 시간(분) */
  retryDelayMinutes: number;
  /** 재출금 안내를 재출금 몇 분 전에 보낼지 */
  retryNoticeLeadMinutes: number;
  /** 야간 방해 금지 구간 [시작, 끝) — "HH:MM" */
  quietStart: string;
  quietEnd: string;
  /** 방해 금지 구간에 걸린 알림을 옮길 전날 저녁 시각 */
  quietFallback: string;
  /** 고객이 고를 수 있는 출금 예정 알림 시점(분) */
  allowedLeads: number[];
}

export function loadConfig(overrides: Partial<AppConfig> = {}): AppConfig {
  return {
    pushqHome: process.env.PUSHQ_HOME ?? join(process.cwd(), ".pushq"),
    pushqBin: join(process.cwd(), "vendor", "bin", "pushq"),
    retryDelayMinutes: 120,
    retryNoticeLeadMinutes: 30,
    quietStart: "22:00",
    quietEnd: "07:00",
    quietFallback: "21:00",
    allowedLeads: [10, 30, 60, 1440],
    ...overrides,
  };
}
