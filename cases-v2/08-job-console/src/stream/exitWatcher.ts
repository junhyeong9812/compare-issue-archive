import { JobEvent } from "../domain/events";
import { ProcessHandle } from "../runtime/process";
import { EventBus } from "../transport/eventBus";
import { Clock } from "../util/clock";

/** Announces process exit as soon as it is observed. */
export class ExitWatcher {
  private closed = false;

  constructor(
    private readonly jobId: string,
    private readonly proc: ProcessHandle,
    private readonly bus: EventBus<JobEvent>,
    private readonly clock: Clock,
    private readonly onExit: (code: number) => void,
  ) {}

  tick(): void {
    if (this.closed) return;
    const code = this.proc.exitCode();
    if (code === null) return;
    this.closed = true;
    this.bus.publish("control", { kind: "closed", jobId: this.jobId, exitCode: code, at: this.clock.now() });
    this.onExit(code);
  }

  isClosed(): boolean {
    return this.closed;
  }
}
