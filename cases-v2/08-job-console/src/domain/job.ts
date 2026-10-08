export type JobStatus = "running" | "succeeded" | "failed";

export interface JobView {
  id: string;
  name: string;
  status: JobStatus;
  exitCode: number | null;
  log: string[];
  seq: number;
  startedAt: number;
  endedAt: number | null;
}

export function statusForExit(code: number): JobStatus {
  return code === 0 ? "succeeded" : "failed";
}

export function isFinished(v: JobView): boolean {
  return v.status !== "running";
}
