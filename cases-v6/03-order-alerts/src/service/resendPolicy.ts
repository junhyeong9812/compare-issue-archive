/**
 * 발송 실패한 알림 가운데 다음 회차에 다시 싣는 것.
 * 여기 든 사유로 실패한 알림은 이벤트를 미발송으로 되돌리고, 소비자도 그 키를 처리한 것으로 치지 않는다.
 * 그 밖의 실패는 회차 리포트의 '실패'로만 남는다.
 */
const RESEND_ON: ReadonlySet<string> = new Set<string>([
  // TODO: 1.9.0부터 비어 있음. 재발송 기능째 걷어낼지, 아직 필요한지 확인 필요
]);

export function shouldResend(reason: string): boolean {
  return RESEND_ON.has(reason);
}
