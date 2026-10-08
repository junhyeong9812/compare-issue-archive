/** What the console can observe about a running child process. */
export interface ProcessHandle {
  /** Raw output chunks written so far (may contain ANSI escapes and \r). */
  output(): string[];
  /** Exit code once the process has exited, otherwise null. */
  exitCode(): number | null;
}

export interface ScriptStep {
  /** Milliseconds after the process was started. */
  at: number;
  write?: string;
  exit?: number;
}

/** Deterministic stand-in for a child process, driven by the console clock. */
export class ScriptedProcess implements ProcessHandle {
  private readonly startedAt: number;

  constructor(private readonly steps: ScriptStep[], private readonly now: () => number) {
    this.startedAt = now();
  }

  output(): string[] {
    const t = this.now() - this.startedAt;
    return this.steps.filter((s) => s.at <= t && s.write !== undefined).map((s) => s.write as string);
  }

  exitCode(): number | null {
    const t = this.now() - this.startedAt;
    const exit = this.steps.find((s) => s.exit !== undefined && s.at <= t);
    return exit ? (exit.exit as number) : null;
  }
}
