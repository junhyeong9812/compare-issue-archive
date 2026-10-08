import { QuietHours } from "../src/scheduling/quietHours";
import { SendTimePolicy } from "../src/scheduling/sendTime";
import { addMinutes, at, compare, isLocalDateTime, previousDate } from "../src/time/localTime";
import { eq, run, test } from "./harness";

test("minute arithmetic crosses days, months and leap days", () => {
  eq(addMinutes("2026-10-12T00:10:00", -30), "2026-10-11T23:40:00", "back over midnight");
  eq(addMinutes("2026-11-01T09:00:00", -1440), "2026-10-31T09:00:00", "back over month end");
  eq(addMinutes("2028-02-28T23:30:00", 60), "2028-02-29T00:30:00", "leap day");
  eq(previousDate("2026-01-01"), "2025-12-31", "previous date over year end");
});

test("local time validation rejects impossible dates", () => {
  eq(isLocalDateTime("2026-02-30T10:00:00"), false, "Feb 30");
  eq(isLocalDateTime("2026-10-12T24:00:00"), false, "hour 24");
  eq(isLocalDateTime("2026-10-12 10:00:00"), false, "space separator");
  eq(isLocalDateTime("2026-10-12T10:00:59"), true, "valid");
  eq(compare(at("2026-10-12", "10:20"), "2026-10-12T10:19:59") > 0, true, "ordering");
});

test("quiet hours pull a reminder back to the previous evening", () => {
  const policy = new SendTimePolicy(new QuietHours("22:00", "07:00", "21:00"));
  eq(policy.sendAt("2026-10-12T07:00:00", 60), "2026-10-11T21:00:00", "06:00 → evening before");
  eq(policy.sendAt("2026-10-12T23:30:00", 30), "2026-10-12T21:00:00", "23:00 → same evening");
  eq(policy.sendAt("2026-10-12T09:00:00", 30), "2026-10-12T08:30:00", "daytime unchanged");
  eq(policy.sendAt("2026-10-12T07:10:00", 10), "2026-10-12T07:00:00", "quiet end is exclusive");
});

run();
