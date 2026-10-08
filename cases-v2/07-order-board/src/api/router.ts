import { OrdersHandler } from "./ordersHandler";
import { Request, Response, notFound, parseUrl } from "./http";

export class Router {
  constructor(private readonly orders: OrdersHandler) {}

  handle(method: Request["method"], url: string, body?: unknown): Response {
    const { path, query } = parseUrl(url);
    const req: Request = { method, path, query, body };
    const parts = path.split("/").filter(Boolean);
    if (parts[0] === "dashboard" && method === "GET") return this.orders.dashboard();
    if (parts[0] !== "orders") return notFound(path);
    if (parts.length === 1) return method === "GET" ? this.orders.list(req) : this.orders.create(body);
    const id = parts[1];
    if (parts.length === 2 && method === "GET") return this.orders.detail(id);
    if (parts.length === 3 && method === "POST") {
      if (parts[2] === "status") return this.orders.setStatus(id, body);
      if (parts[2] === "payment") return this.orders.payment(id, body);
      if (parts[2] === "shipment") return this.orders.shipment(id, body);
    }
    return notFound(path);
  }
}
