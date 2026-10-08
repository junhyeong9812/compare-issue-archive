import { JobsHandler } from "./api/jobsHandler";
import { Router } from "./api/router";
import { ConsoleConfig, defaultConfig } from "./config";
import { JobEvent } from "./domain/events";
import { ProcessHandle } from "./runtime/process";
import { JobProjection } from "./projection/jobProjection";
import { Launcher, ProcessFactory } from "./service/launcher";
import { RetentionService } from "./service/retention";
import { JobViewStore } from "./store/jobViewStore";
import { Supervisor } from "./stream/supervisor";
import { EventBus } from "./transport/eventBus";
import { ManualClock } from "./util/clock";

export interface ConsoleApp {
  router: Router;
  clock: ManualClock;
  /** One host-loop iteration: advance time, poll jobs, deliver events. */
  tick(): void;
  /** Runs host-loop iterations for `ms` of simulated time. */
  runFor(ms: number): void;
  activeJobs(): string[];
}

export function createConsole(spawn: (command: string, now: () => number) => ProcessHandle, overrides: Partial<ConsoleConfig> = {}): ConsoleApp {
  const config: ConsoleConfig = { ...defaultConfig, ...overrides };
  const clock = new ManualClock();
  const bus = new EventBus<JobEvent>();
  const store = new JobViewStore();
  const projection = new JobProjection(store);
  bus.subscribe((e) => projection.apply(e));
  const supervisor = new Supervisor(bus, clock, config);
  const factory: ProcessFactory = (command) => spawn(command, () => clock.now());
  const launcher = new Launcher(supervisor, factory);
  const retention = new RetentionService(store, clock, config.retainFinishedMs);
  const router = new Router(new JobsHandler(store, launcher, projection, config));

  const tick = () => {
    clock.advance(config.tickMs);
    supervisor.tick();
    bus.drain();
    retention.sweep();
  };
  return {
    router,
    clock,
    tick,
    runFor(ms: number) {
      const end = clock.now() + ms;
      while (clock.now() < end) tick();
    },
    activeJobs: () => supervisor.activeJobs(),
  };
}
