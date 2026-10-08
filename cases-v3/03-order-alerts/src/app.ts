import { mountAlertRoutes, mountOrderRoutes } from "./api/routes";
import { Response, Router } from "./api/router";
import { AppConfig, loadConfig } from "./config";
import { AlertPublisher } from "./messaging/eventPublisher";
import { MqpubClient } from "./messaging/mqpubClient";
import { AlertConsumer } from "./notifier/consumerWorker";
import { InMemorySmsGateway } from "./notifier/smsGateway";
import { CustomerRepository } from "./repository/customerRepository";
import { DeliveryLogRepository } from "./repository/deliveryLogRepository";
import { OrderRepository } from "./repository/orderRepository";
import { DispatchService } from "./service/dispatchService";
import { OrderService } from "./service/orderService";

export interface App {
  config: AppConfig;
  handle(method: string, url: string, body?: unknown): Response;
}

export function createApp(overrides: Partial<AppConfig> = {}): App {
  const config = loadConfig(overrides);
  const customers = new CustomerRepository();
  const orders = new OrderRepository();
  const log = new DeliveryLogRepository();
  const gateway = new InMemorySmsGateway();
  const client = new MqpubClient(config.mqpubBin, config.spoolDir);
  const publisher = new AlertPublisher(client, config.topic, config.keyNamespace);
  const consumer = new AlertConsumer(client, config.topic, customers, gateway, log, config.senderId, config.drainLimit);
  const dispatch = new DispatchService(orders, customers, publisher, consumer, log);

  const router = new Router();
  mountOrderRoutes(router, new OrderService(customers, orders), orders);
  mountAlertRoutes(router, dispatch, log, customers, gateway);
  return { config, handle: (m, u, b) => router.handle(m, u, b) };
}
