import { RevisionStore } from "../src/store/revisionStore";
import { changedKeys } from "../src/util/diff";
import { eq, run, test } from "./harness";

const profile = (deviceId: string, gain: number) => ({ deviceId, name: "n", firmware: "1.0.0", settings: { gain } });

test("revisions are numbered per device and listed newest first", () => {
  const store = new RevisionStore();
  store.append("a", profile("a", 1), "report", 10, ["gain"]);
  store.append("b", profile("b", 1), "report", 11, ["gain"]);
  store.append("a", profile("a", 2), "import", 12, ["gain"]);
  eq(store.latest("a")?.rev, 2, "latest a");
  eq(store.latest("b")?.rev, 1, "latest b");
  eq(store.history("a", 10).map((r) => [r.rev, r.source]), [[2, "import"], [1, "report"]], "history");
  eq(store.history("a", 1).length, 1, "limit");
});

test("changed keys cover added, removed and modified settings", () => {
  eq(changedKeys({ a: 1, b: [1, 2], c: "x" }, { a: 1, b: [1, 3], d: true }), ["b", "c", "d"], "diff");
  eq(changedKeys({ a: { x: 1, y: 2 } }, { a: { y: 2, x: 1 } }), [], "key order does not matter");
  eq(changedKeys({ uptimeSec: 1 }, { uptimeSec: 2 }, ["uptimeSec"]), [], "ignored");
});

run();
