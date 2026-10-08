import { ConsoleConfig } from "../config";
import { JobEvent } from "../domain/events";
import { ProcessHandle } from "../runtime/process";
import { OutputTail } from "../runtime/tail";
import { EventBus } from "../transport/eventBus";
import { Clock } from "../util/clock";
import { ExitWatcher } from "./exitWatcher";
import { LogPump } from "./logPump";

interface Running {
  pump: LogPump;
  watcher: ExitWatcher;
}

/** Owns the per-job pump and exit watcher; driven by the host loop. */
export class Supervisor {
  private readonly running = new Map<string, Running>();

  constructor(
    private readonly bus: EventBus<JobEvent>,
    private readonly clock: Clock,
    private readonly config: ConsoleConfig,
  ) {}

  attach(jobId: string, name: string, proc: ProcessHandle): void {
    if (this.running.has(jobId)) throw new Error(`job ${jobId} already attached`);
    const pump = new LogPump(jobId, new OutputTail(proc), this.bus, this.clock, this.config);
    const watcher = new ExitWatcher(jobId, proc, this.bus, this.clock, () => this.detach(jobId));
    this.running.set(jobId, { pump, watcher });
    this.bus.publish("control", { kind: "started", jobId, name, at: this.clock.now() });
  }

  tick(): void {
    for (const { pump, watcher } of [...this.running.values()]) {
      pump.tick();
      watcher.tick();
    }
  }

  activeJobs(): string[] {
    return [...this.running.keys()];
  }

  private detach(jobId: string): void {
    const r = this.running.get(jobId);
    if (!r) return;
    r.pump.stop();
    this.running.delete(jobId);
  }
}
