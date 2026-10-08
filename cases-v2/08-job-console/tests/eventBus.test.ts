import { EventBus } from "../src/transport/eventBus";
import { eq, run, test } from "./harness";

test("each lane is first-in first-out", () => {
  const bus = new EventBus<string>();
  const got: string[] = [];
  bus.subscribe((m) => got.push(m));
  bus.publish("data", "d1");
  bus.publish("data", "d2");
  bus.publish("control", "c1");
  bus.publish("control", "c2");
  eq(bus.drain(), 4, "count");
  eq(got.filter((m) => m.startsWith("d")), ["d1", "d2"], "data order");
  eq(got.filter((m) => m.startsWith("c")), ["c1", "c2"], "control order");
});

test("messages published while draining wait for the next drain", () => {
  const bus = new EventBus<string>();
  const got: string[] = [];
  bus.subscribe((m) => {
    got.push(m);
    if (m === "a") bus.publish("control", "b");
  });
  bus.publish("data", "a");
  bus.drain();
  eq(got, ["a"], "first drain");
  eq(bus.pending(), 1, "queued");
  bus.drain();
  eq(got, ["a", "b"], "second drain");
});

test("unsubscribe stops delivery", () => {
  const bus = new EventBus<number>();
  let n = 0;
  const off = bus.subscribe(() => n++);
  bus.publish("data", 1);
  bus.drain();
  off();
  bus.publish("data", 2);
  bus.drain();
  eq(n, 1, "deliveries");
});

run();
