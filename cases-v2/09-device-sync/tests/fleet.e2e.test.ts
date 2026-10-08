import { createApp } from "../src/app";
import { StepClock, eq, run, test } from "./harness";

function sensorProfile(offset: number) {
  return {
    deviceId: "boiler-7",
    name: "Boiler 7",
    firmware: "3.2.0",
    settings: {
      targetTemp: 72.5,
      maxPressure: 3.1,
      sensors: [
        { id: "t-in", offset: 0.15, gain: 1.02 },
        { id: "t-out", offset, gain: 0.98 },
      ],
      mode: "eco",
      uptimeSec: 100,
    },
  };
}

test("repeated heartbeats with an unchanged configuration add no history", () => {
  const app = createApp(new StepClock());
  eq(app.handle("POST", "/devices/boiler-7/report", sensorProfile(0.4)).status, 201, "first report");
  for (let i = 0; i < 3; i++) {
    const p = sensorProfile(0.4);
    p.settings.uptimeSec += i * 60;
    eq(app.handle("POST", "/devices/boiler-7/report", p).body, { rev: 1, changed: false }, `heartbeat ${i}`);
  }
});

test("a device whose sensor could not be calibrated stops growing history after its first report", () => {
  const app = createApp(new StepClock());
  for (let i = 0; i < 4; i++) app.handle("POST", "/devices/boiler-7/report", sensorProfile(Number.NaN));
  const h = app.handle("GET", "/devices/boiler-7/revisions").body as { count: number };
  eq(h.count, 1, "revision count");
});

test("status page reports in sync when the device runs exactly what was requested", () => {
  const app = createApp(new StepClock());
  app.handle("PUT", "/devices/boiler-7/desired", sensorProfile(Number.NaN));
  app.handle("POST", "/devices/boiler-7/report", sensorProfile(Number.NaN));
  const s = app.handle("GET", "/devices/boiler-7/status").body as { state: string; drift: string[] };
  eq([s.state, s.drift], ["in-sync", []], "status");
});

run();
