import { at, dateOf, LocalDateTime, previousDate, timeOf } from "../time/localTime";

/**
 * 야간 방해 금지 — [start, end) 구간(자정을 넘는다)에 걸린 알림은
 * 늦게 보내지 않고 직전 저녁(fallback 시각)으로 당긴다. 금융 알림은 늦으면 안 되기 때문이다.
 */
export class QuietHours {
  constructor(
    private readonly start: string,
    private readonly end: string,
    private readonly fallback: string,
  ) {}

  contains(t: LocalDateTime): boolean {
    const hm = timeOf(t);
    return this.start > this.end ? hm >= this.start || hm < this.end : hm >= this.start && hm < this.end;
  }

  /** 구간 밖이면 그대로, 안이면 직전 저녁 fallback 시각 */
  pullBack(t: LocalDateTime): LocalDateTime {
    if (!this.contains(t)) return t;
    const d = dateOf(t);
    const eveningOf = timeOf(t) >= this.start ? d : previousDate(d);
    return at(eveningOf, this.fallback);
  }
}
