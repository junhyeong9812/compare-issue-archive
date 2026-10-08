import { paginate, sortBy } from "../src/util/collections";
import { formatWon } from "../src/util/money";
import { eq, run, test } from "./harness";

test("formats thousands", () => {
  eq(formatWon(0), "0원", "zero");
  eq(formatWon(999), "999원", "below 1000");
  eq(formatWon(1000), "1,000원", "1000");
  eq(formatWon(1234567), "1,234,567원", "millions");
});

test("negative amounts keep the sign in front", () => {
  eq(formatWon(-45000), "-45,000원", "refund");
});

test("german locale uses dots", () => {
  eq(formatWon(1234567, "de-DE"), "1.234.567원", "de");
  eq(formatWon(1234567, "ko-KR"), "1,234,567원", "ko after de");
});

test("pages are 1-based and report hasNext", () => {
  const all = [1, 2, 3, 4, 5];
  eq(paginate(all, 1, 2), { items: [1, 2], page: 1, pageSize: 2, total: 5, hasNext: true }, "p1");
  eq(paginate(all, 3, 2), { items: [5], page: 3, pageSize: 2, total: 5, hasNext: false }, "p3");
  eq(paginate(all, 0, 2).page, 1, "clamped");
});

test("sortBy is stable and multi-key", () => {
  const rows = [
    { n: "a", p: 1, t: 3 },
    { n: "b", p: 0, t: 1 },
    { n: "c", p: 1, t: 3 },
    { n: "d", p: 1, t: 9 },
  ];
  eq(sortBy(rows, (r) => r.p, (r) => -r.t).map((r) => r.n), ["b", "d", "a", "c"], "order");
});

run();
