/**
 * In-process message bus shared with other services.
 *
 * Two lanes: "control" for small lifecycle messages and "data" for bulk payloads.
 * Each lane is FIFO. On drain, control messages are delivered before data so that
 * lifecycle changes are never stuck behind large payloads. No ordering is promised
 * between the two lanes.
 */
export type Lane = "control" | "data";

export type Subscriber<T> = (msg: T) => void;

export class EventBus<T> {
  private readonly queues: Record<Lane, T[]> = { control: [], data: [] };
  private readonly subscribers: Subscriber<T>[] = [];
  private delivered = 0;

  publish(lane: Lane, msg: T): void {
    this.queues[lane].push(msg);
  }

  subscribe(fn: Subscriber<T>): () => void {
    this.subscribers.push(fn);
    return () => {
      const i = this.subscribers.indexOf(fn);
      if (i >= 0) this.subscribers.splice(i, 1);
    };
  }

  /** Delivers everything queued so far. Messages published during delivery wait for the next drain. */
  drain(): number {
    const batch = [...this.queues.control, ...this.queues.data];
    this.queues.control = [];
    this.queues.data = [];
    for (const msg of batch) {
      for (const s of [...this.subscribers]) s(msg);
    }
    this.delivered += batch.length;
    return batch.length;
  }

  pending(): number {
    return this.queues.control.length + this.queues.data.length;
  }

  stats(): { delivered: number } {
    return { delivered: this.delivered };
  }
}
