import { JobsHandler, Response } from "./jobsHandler";

export class Router {
  constructor(private readonly jobs: JobsHandler) {}

  handle(method: "GET" | "POST", path: string, body?: unknown): Response {
    const parts = path.split("/").filter(Boolean);
    if (parts[0] === "events" && method === "POST") return this.jobs.ingest(body);
    if (parts[0] !== "jobs") return { status: 404, body: { error: path } };
    if (parts.length === 1) return method === "GET" ? this.jobs.list() : this.jobs.launch(body);
    if (parts.length === 2 && method === "GET") return this.jobs.detail(parts[1]);
    return { status: 404, body: { error: path } };
  }
}
