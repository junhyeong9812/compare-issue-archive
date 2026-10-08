import { ConsoleConfig } from "../config";
import { JobEvent } from "../domain/events";
import { OutputTail } from "../runtime/tail";
import { EventBus } from "../transport/eventBus";
import { Clock } from "../util/clock";
import { fingerprint } from "../util/fingerprint";

/**
 * Polls one job's output every tick and publishes log snapshots.
 * Snapshots are coalesced: at most one per `emitMinIntervalMs`; a change that
 * arrives inside the window goes out with the first tick after it.
 */
export class LogPump {
  private lastFp: string | null = null;
  private unsent: string[] | null = null;
  private lastEmit = Number.NEGATIVE_INFINITY;
  private seq = 0;
  private stopped = false;

  constructor(
    private readonly jobId: string,
    private readonly tail: OutputTail,
    private readonly bus: EventBus<JobEvent>,
    private readonly clock: Clock,
    private readonly config: ConsoleConfig,
  ) {}

  tick(): void {
    if (this.stopped) return;
    const lines = this.tail.read();
    const fp = fingerprint(lines);
    if (fp !== this.lastFp) {
      this.lastFp = fp;
      this.unsent = lines;
    }
    const now = this.clock.now();
    if (this.unsent !== null && now - this.lastEmit >= this.config.emitMinIntervalMs) {
      this.bus.publish("data", { kind: "timeline", jobId: this.jobId, seq: ++this.seq, lines: this.unsent });
      this.unsent = null;
      this.lastEmit = now;
    }
  }

  stop(): void {
    this.stopped = true;
  }

  sent(): number {
    return this.seq;
  }
}
