export interface ConsoleConfig {
  /** Host loop period. */
  tickMs: number;
  /** Log snapshots for one job are sent at most this often (bandwidth). */
  emitMinIntervalMs: number;
  /** Lines returned by the job detail endpoint (tail). */
  maxLogLines: number;
  /** Finished jobs are dropped from the console after this long. */
  retainFinishedMs: number;
}

export const defaultConfig: ConsoleConfig = {
  tickMs: 100,
  emitMinIntervalMs: 400,
  maxLogLines: 200,
  retainFinishedMs: 60 * 60 * 1000,
};
