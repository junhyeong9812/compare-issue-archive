import { formatNumber, roundTo } from "../src/codec/numberFormat";
import { decodeProfile, encodeProfile } from "../src/codec/settingsCodec";
import { eq, run, test, truthy } from "./harness";

test("profiles survive the storage encoding, including non-finite numbers", () => {
  const p = {
    deviceId: "d-1",
    name: "pump",
    firmware: "2.4.1",
    settings: { gain: 1.25, ceiling: Number.POSITIVE_INFINITY, floor: Number.NEGATIVE_INFINITY, bias: Number.NaN, tags: ["a", "b"] },
  };
  const back = decodeProfile(encodeProfile(p));
  eq(back.settings.gain, 1.25, "gain");
  eq(back.settings.ceiling === Number.POSITIVE_INFINITY, true, "ceiling");
  eq(back.settings.floor === Number.NEGATIVE_INFINITY, true, "floor");
  truthy(Number.isNaN(back.settings.bias as number), "bias should stay NaN");
  eq(back.settings.tags, ["a", "b"], "tags");
});

test("rounding is stable and leaves non-finite values alone", () => {
  eq(roundTo(0.1 + 0.2, 6), 0.3, "0.1+0.2");
  eq(roundTo(roundTo(2.0000004, 6), 6), 2, "idempotent");
  eq(roundTo(-0.0000001, 6), 0, "no negative zero");
  truthy(Number.isNaN(roundTo(Number.NaN, 6)), "NaN stays NaN");
  eq(roundTo(Number.POSITIVE_INFINITY, 6), Number.POSITIVE_INFINITY, "inf");
});

test("display formatting", () => {
  eq([formatNumber(Number.NaN), formatNumber(Number.POSITIVE_INFINITY), formatNumber(1.5)], ["n/a", "∞", "1.5"], "fmt");
});

run();
