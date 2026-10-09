import { addMinutes, LocalDateTime } from "../time/localTime";
import { QuietHours } from "./quietHours";

/**
 * 알림 발송 시각 정책.
 * - 안내 대상 시각(출금·재출금)보다 lead 분 앞서 보낸다.
 * - 야간 방해 금지 구간에 걸리면 전날 저녁으로 당긴다.
 * 결과는 그대로 푸시 예약 시각이 된다.
 */
export class SendTimePolicy {
  constructor(private readonly quiet: QuietHours) {}

  sendAt(dueAt: LocalDateTime, leadMinutes: number): LocalDateTime {
    const nominal = addMinutes(dueAt, -leadMinutes);
    return this.quiet.pullBack(nominal);
  }
}
