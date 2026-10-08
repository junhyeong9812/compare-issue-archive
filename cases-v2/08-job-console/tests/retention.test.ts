import { JobViewStore } from "../src/store/jobViewStore";
import { RetentionService } from "../src/service/retention";
import { ManualClock } from "../src/util/clock";
import { eq, run, test } from "./harness";

test("finished jobs past the window are dropped, running ones stay", () => {
  const clock = new ManualClock(10_000);
  const store = new JobViewStore();
  const base = { name: "n", exitCode: 0, log: [], seq: 1, startedAt: 0 };
  store.put({ ...base, id: "old", status: "succeeded", endedAt: 1_000 });
  store.put({ ...base, id: "recent", status: "failed", endedAt: 9_500 });
  store.put({ ...base, id: "live", status: "running", exitCode: null, endedAt: null });
  const removed = new RetentionService(store, clock, 5_000).sweep();
  eq(removed, ["old"], "removed");
  eq(store.list().map((v) => v.id).sort(), ["live", "recent"], "kept");
});

run();
