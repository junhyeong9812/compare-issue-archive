import { join } from "path";

export interface AppConfig {
  /** 고객 알림 토픽 */
  topic: string;
  /** 사내 메시지 키 네임스페이스 */
  keyNamespace: string;
  /** mqpub 실행 파일 경로 */
  mqpubBin: string;
  /** mqpub 스풀 디렉토리(MQPUB_SPOOL) */
  spoolDir: string;
  /** 문자 발신 번호 */
  senderId: string;
  /** 한 번에 읽어 올 최대 메시지 수 */
  drainLimit: number;
  /** 모의 문자 게이트웨이 장애 주입(개발·스테이징): 번호별로 차례대로 돌려줄 실패 코드 */
  smsFaults: Record<string, string[]>;
}

export function loadConfig(overrides: Partial<AppConfig> = {}): AppConfig {
  const env = process.env;
  const base: AppConfig = {
    topic: env.ALERTS_TOPIC ?? "alerts.order",
    keyNamespace: env.ALERTS_KEY_NS ?? "ntf",
    mqpubBin: env.MQPUB_BIN ?? join(process.cwd(), "vendor", "bin", "mqpub"),
    spoolDir: env.MQPUB_SPOOL ?? join(process.cwd(), "var", "spool"),
    senderId: "1588-0000",
    drainLimit: 500,
    smsFaults: {},
  };
  return { ...base, ...overrides };
}
