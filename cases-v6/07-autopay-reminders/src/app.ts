import { mountAccountRoutes, mountReminderRoutes } from "./api/routes";
import { Response, Router } from "./api/router";
import { AppConfig, loadConfig } from "./config";
import { PushqClient } from "./push/pushqClient";
import { ReminderReportBuilder } from "./report/reminderReport";
import { AutopayRepository } from "./repository/autopayRepository";
import { MemberRepository } from "./repository/memberRepository";
import { ReminderRepository } from "./repository/reminderRepository";
import { QuietHours } from "./scheduling/quietHours";
import { SendTimePolicy } from "./scheduling/sendTime";
import { ReminderPlanner } from "./service/reminderPlanner";
import { ReminderService } from "./service/reminderService";

export interface App {
  config: AppConfig;
  handle(method: string, url: string, body?: unknown): Response;
}

export function createApp(overrides: Partial<AppConfig> = {}): App {
  const config = loadConfig(overrides);
  const members = new MemberRepository();
  const autopays = new AutopayRepository();
  const reminders = new ReminderRepository();
  const push = new PushqClient(config.pushqBin, config.pushqHome);
  const policy = new SendTimePolicy(new QuietHours(config.quietStart, config.quietEnd, config.quietFallback));
  const planner = new ReminderPlanner(members, autopays, policy, config.retryNoticeLeadMinutes);
  const service = new ReminderService(planner, autopays, reminders, push, config.retryDelayMinutes);
  const report = new ReminderReportBuilder(reminders, push);

  const router = new Router();
  mountAccountRoutes(router, config, members, autopays);
  mountReminderRoutes(router, service, report, members, push);
  return { config, handle: (m, u, b) => router.handle(m, u, b) };
}
