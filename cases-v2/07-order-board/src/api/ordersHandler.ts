import { AppConfig } from "../config";
import { isOrderStatus } from "../domain/order";
import { isPaymentStatus } from "../domain/payment";
import { isShipmentStatus } from "../domain/shipment";
import { summarize } from "../presenter/dashboardPresenter";
import { presentRow } from "../presenter/orderRowPresenter";
import { OrderCommandService } from "../service/orderCommandService";
import { OrderQueryService } from "../service/orderQueryService";
import { paginate } from "../util/collections";
import { Request, Response, badRequest, notFound, ok } from "./http";

export class OrdersHandler {
  constructor(
    private readonly query: OrderQueryService,
    private readonly commands: OrderCommandService,
    private readonly config: AppConfig,
  ) {}

  list(req: Request): Response {
    const status = req.query.status;
    if (status !== undefined && !isOrderStatus(status)) return badRequest(`unknown status ${status}`);
    const rows = this.query.find({ status, customer: req.query.customer }).map((a) => presentRow(a, this.config.locale));
    const page = Number(req.query.page ?? "1");
    return ok(paginate(rows, page, this.config.pageSize));
  }

  detail(id: string): Response {
    const agg = this.query.byId(id);
    return agg ? ok(presentRow(agg, this.config.locale)) : notFound(`order ${id}`);
  }

  dashboard(): Response {
    const rows = this.query.find().map((a) => presentRow(a, this.config.locale));
    return ok(summarize(rows));
  }

  create(body: unknown): Response {
    const b = body as { customer?: string; lines?: { sku: string; qty: number; unitPrice: number }[] };
    try {
      return ok(this.commands.place(b.customer ?? "", b.lines ?? []));
    } catch (e) {
      return badRequest((e as Error).message);
    }
  }

  setStatus(id: string, body: unknown): Response {
    const to = (body as { status?: string }).status ?? "";
    if (!isOrderStatus(to)) return badRequest(`unknown status ${to}`);
    try {
      return ok(this.commands.transition(id, to));
    } catch (e) {
      return badRequest((e as Error).message);
    }
  }

  payment(id: string, body: unknown): Response {
    const b = body as { status?: string; amount?: number };
    if (!b.status || !isPaymentStatus(b.status)) return badRequest(`unknown payment status ${b.status}`);
    try {
      this.commands.recordPayment(id, b.status, b.amount ?? 0);
      return ok({ recorded: true });
    } catch (e) {
      return notFound((e as Error).message);
    }
  }

  shipment(id: string, body: unknown): Response {
    const b = body as { status?: string; carrier?: string };
    if (!b.status || !isShipmentStatus(b.status)) return badRequest(`unknown shipment status ${b.status}`);
    try {
      this.commands.recordShipment(id, b.status, b.carrier);
      return ok({ recorded: true });
    } catch (e) {
      return notFound((e as Error).message);
    }
  }
}
