import { JobEvent } from "../domain/events";
import { JobView, statusForExit } from "../domain/job";
import { JobViewStore } from "../store/jobViewStore";

/** Folds job events into the read model the console API serves. */
export class JobProjection {
  constructor(private readonly store: JobViewStore) {}

  apply(e: JobEvent): void {
    switch (e.kind) {
      case "started":
        this.store.put({
          id: e.jobId,
          name: e.name,
          status: "running",
          exitCode: null,
          log: [],
          seq: 0,
          startedAt: e.at,
          endedAt: null,
        });
        return;
      case "timeline": {
        const view = this.store.get(e.jobId) ?? this.blank(e.jobId);
        if (e.seq <= view.seq) return; // stale snapshot
        this.store.put({ ...view, status: "running", log: e.lines, seq: e.seq });
        return;
      }
      case "closed": {
        const view = this.store.get(e.jobId) ?? this.blank(e.jobId);
        this.store.put({ ...view, status: statusForExit(e.exitCode), exitCode: e.exitCode, endedAt: e.at });
        return;
      }
    }
  }

  private blank(id: string): JobView {
    return { id, name: id, status: "running", exitCode: null, log: [], seq: 0, startedAt: 0, endedAt: null };
  }
}
