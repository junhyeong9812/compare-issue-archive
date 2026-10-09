import { Autopay } from "../domain/autopay";
import { ReminderKind } from "../domain/reminder";
import { LocalDateTime, timeOf } from "../time/localTime";
import { shortMerchant, won } from "../util/money";

function whenText(dueAt: LocalDateTime): string {
  const [, mo, d] = dueAt.slice(0, 10).split("-").map(Number);
  return `${mo}/${d} ${timeOf(dueAt)}`;
}

export function renderMessage(kind: ReminderKind, autopay: Autopay, dueAt: LocalDateTime): string {
  const m = shortMerchant(autopay.merchant);
  const amount = won(autopay.amount);
  switch (kind) {
    case "BILL_DUE":
      return `[한빛페이] ${m} 자동이체 ${amount}이 ${whenText(dueAt)}에 출금됩니다. 잔액을 확인해 주세요.`;
    case "RETRY":
      return `[한빛페이] ${m} 자동이체 ${amount} 출금이 실패해 ${whenText(dueAt)}에 다시 출금합니다.`;
  }
}
