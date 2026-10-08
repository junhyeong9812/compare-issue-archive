import { isFinished } from "../domain/job";
import { JobViewStore } from "../store/jobViewStore";
import { Clock } from "../util/clock";

/** Drops finished jobs from the console once they are older than the retention window. */
export class RetentionService {
  constructor(private readonly store: JobViewStore, private readonly clock: Clock, private readonly retainMs: number) {}

  sweep(): string[] {
    const now = this.clock.now();
    const removed: string[] = [];
    for (const v of this.store.list()) {
      if (!isFinished(v) || v.endedAt === null) continue;
      if (now - v.endedAt > this.retainMs) {
        this.store.delete(v.id);
        removed.push(v.id);
      }
    }
    return removed;
  }
}
