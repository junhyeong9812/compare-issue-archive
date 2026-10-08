import { OrdersHandler } from "./api/ordersHandler";
import { Router } from "./api/router";
import { AppConfig, defaultConfig } from "./config";
import { OrderRepository } from "./repository/orderRepository";
import { PaymentRepository } from "./repository/paymentRepository";
import { ShipmentRepository } from "./repository/shipmentRepository";
import { Clock, OrderCommandService } from "./service/orderCommandService";
import { OrderQueryService } from "./service/orderQueryService";

export function createApp(clock: Clock, overrides: Partial<AppConfig> = {}): Router {
  const config = { ...defaultConfig, ...overrides };
  const orders = new OrderRepository();
  const payments = new PaymentRepository();
  const shipments = new ShipmentRepository();
  const query = new OrderQueryService(orders, payments, shipments);
  const commands = new OrderCommandService(orders, payments, shipments, clock);
  return new Router(new OrdersHandler(query, commands, config));
}
