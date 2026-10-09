import { AppConfig } from "../config";
import { validateAutopay } from "../domain/autopay";
import { validateMember } from "../domain/member";
import { normalizeDevice } from "../push/deviceToken";
import { PushqClient } from "../push/pushqClient";
import { ReminderReportBuilder } from "../report/reminderReport";
import { AutopayRepository } from "../repository/autopayRepository";
import { MemberRepository } from "../repository/memberRepository";
import { ReminderService } from "../service/reminderService";
import { isLocalDateTime } from "../time/localTime";
import { notFound, ok, Router } from "./router";

const DATE = /^\d{4}-\d{2}-\d{2}$/;

export function mountAccountRoutes(router: Router, config: AppConfig, members: MemberRepository, autopays: AutopayRepository): void {
  router.add("POST", "/members", (_p, body) => {
    const m = validateMember(body, config.allowedLeads);
    return ok(members.save({ ...m, device: normalizeDevice(m.device) }));
  });

  router.add("POST", "/autopays", (_p, body) => {
    const input = validateAutopay(body);
    if (!members.find(input.memberId)) return notFound(`member ${input.memberId}`);
    return ok(autopays.create(input));
  });
}

export function mountReminderRoutes(
  router: Router,
  service: ReminderService,
  report: ReminderReportBuilder,
  members: MemberRepository,
  push: PushqClient,
): void {
  router.add("POST", "/reminders/plan", (_p, body) => {
    const date = (body as { date?: unknown } | undefined)?.date;
    if (typeof date !== "string" || !DATE.test(date)) throw new Error("date must be YYYY-MM-DD");
    return ok(service.planDay(date));
  });

  router.add("POST", "/autopays/:id/failures", (p, body) => ok(service.recordFailure(p.id, body)));

  router.add("POST", "/autopays/:id/cancel", (p) => ok(service.cancelAutopay(p.id)));

  router.add("POST", "/clock", (_p, body) => {
    const until = (body as { until?: unknown } | undefined)?.until;
    if (!isLocalDateTime(until)) throw new Error("until must be YYYY-MM-DDTHH:MM:SS");
    return ok(service.advance(until));
  });

  router.add("GET", "/reports/reminders/:date", (p) => {
    if (!DATE.test(p.date)) throw new Error("date must be YYYY-MM-DD");
    return ok(report.build(p.date, service.now()));
  });

  router.add("GET", "/members/:id/notifications", (p) => {
    const m = members.find(p.id);
    if (!m) return notFound(`member ${p.id}`);
    const notifications = push.delivered(m.device).map((d) => ({ at: d.at, tag: d.tag, message: d.message }));
    return ok({ memberId: m.id, notifications });
  });
}
