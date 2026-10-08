import { ConsoleConfig } from "../config";
import { isJobEvent } from "../domain/events";
import { JobView } from "../domain/job";
import { JobProjection } from "../projection/jobProjection";
import { Launcher } from "../service/launcher";
import { JobViewStore } from "../store/jobViewStore";

export interface Response {
  status: number;
  body: unknown;
}

export interface JobDetail {
  id: string;
  name: string;
  status: string;
  exitCode: number | null;
  log: string[];
  truncated: boolean;
}

export class JobsHandler {
  constructor(
    private readonly store: JobViewStore,
    private readonly launcher: Launcher,
    private readonly projection: JobProjection,
    private readonly config: ConsoleConfig,
  ) {}

  detail(id: string): Response {
    const v = this.store.get(id);
    if (!v) return { status: 404, body: { error: `no job ${id}` } };
    return { status: 200, body: this.present(v) };
  }

  list(): Response {
    const rows = this.store.list().map((v) => ({ id: v.id, name: v.name, status: v.status, lines: v.log.length }));
    return { status: 200, body: rows };
  }

  launch(body: unknown): Response {
    const b = (body ?? {}) as { name?: string; command?: string };
    try {
      return { status: 201, body: { id: this.launcher.launch(b.name ?? "", b.command ?? "") } };
    } catch (e) {
      return { status: 400, body: { error: (e as Error).message } };
    }
  }

  /** Events forwarded by remote runners (other hosts). They arrive in whatever order the network delivers. */
  ingest(body: unknown): Response {
    const events = Array.isArray(body) ? body : [body];
    let accepted = 0;
    for (const e of events) {
      if (!isJobEvent(e)) continue;
      this.projection.apply(e);
      accepted++;
    }
    return { status: 202, body: { accepted } };
  }

  private present(v: JobView): JobDetail {
    const max = this.config.maxLogLines;
    const truncated = v.log.length > max;
    return {
      id: v.id,
      name: v.name,
      status: v.status,
      exitCode: v.exitCode,
      log: truncated ? v.log.slice(v.log.length - max) : v.log,
      truncated,
    };
  }
}
