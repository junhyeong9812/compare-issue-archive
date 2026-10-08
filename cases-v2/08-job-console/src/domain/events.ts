export interface StartedEvent {
  kind: "started";
  jobId: string;
  name: string;
  at: number;
}

/** Full log snapshot so far. `seq` increases per job. */
export interface TimelineEvent {
  kind: "timeline";
  jobId: string;
  seq: number;
  lines: string[];
}

export interface ClosedEvent {
  kind: "closed";
  jobId: string;
  exitCode: number;
  at: number;
}

export type JobEvent = StartedEvent | TimelineEvent | ClosedEvent;

export function isJobEvent(x: unknown): x is JobEvent {
  if (typeof x !== "object" || x === null) return false;
  const e = x as { kind?: unknown; jobId?: unknown };
  if (typeof e.jobId !== "string") return false;
  return e.kind === "started" || e.kind === "timeline" || e.kind === "closed";
}
