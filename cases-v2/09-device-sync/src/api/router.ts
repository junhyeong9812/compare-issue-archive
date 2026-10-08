import { DevicesHandler, Response } from "./devicesHandler";

export class Router {
  constructor(private readonly devices: DevicesHandler) {}

  handle(method: "GET" | "POST" | "PUT", path: string, body?: unknown): Response {
    const parts = path.split("/").filter(Boolean);
    if (parts[0] === "import" && method === "POST") return this.devices.import(body);
    if (parts[0] !== "devices" || parts.length !== 3) return { status: 404, body: { error: path } };
    const [, id, action] = parts;
    if (action === "report" && method === "POST") return this.devices.report(id, body);
    if (action === "revisions" && method === "GET") return this.devices.history(id);
    if (action === "desired" && method === "PUT") return this.devices.setDesired(id, body);
    if (action === "status" && method === "GET") return this.devices.status(id);
    return { status: 404, body: { error: path } };
  }
}
