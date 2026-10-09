import { addMinutes, fromEpochSeconds, LocalDateTime, toEpochSeconds } from "../time/localTime";
import { QuietHours } from "./quietHours";

/** pushq 는 5분 경계(분이 5의 배수, 초 00)가 아닌 예약 시각을 티켓만 주고 큐에 넣지 않는다. */
const PUSH_SLOT_SECONDS = 300;

/**
 * 알림 발송 시각 정책.
 * - 안내 대상 시각(출금·재출금)보다 lead 분 앞서 보낸다.
 * - 야간 방해 금지 구간에 걸리면 전날 저녁으로 당긴다.
 * - 푸시 예약 시각은 직전 5분 경계로 내린다(늦게 보내지 않는다).
 */
export class SendTimePolicy {
  constructor(private readonly quiet: QuietHours) {}

  sendAt(dueAt: LocalDateTime, leadMinutes: number): LocalDateTime {
    const nominal = addMinutes(dueAt, -leadMinutes);
    return floorToSlot(this.quiet.pullBack(nominal));
  }
}

function floorToSlot(t: LocalDateTime): LocalDateTime {
  const s = toEpochSeconds(t);
  return fromEpochSeconds(s - (s % PUSH_SLOT_SECONDS));
}
