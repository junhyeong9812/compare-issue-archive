import { formatOrderNo } from "../src/domain/order";
import { decodePayload, encodePayload } from "../src/messaging/payloadCodec";
import { renderAlert } from "../src/notifier/templates";
import { clip, displayWidth, maskMobile, normalizeMobile } from "../src/util/format";
import { eq, run, test } from "./harness";

test("mobile numbers normalize to 11 digits", () => {
  eq(normalizeMobile("010-1234-5678"), "01012345678", "dashes");
  eq(normalizeMobile("+82 10 1234 5678"), "01012345678", "country code");
  eq(normalizeMobile("8201012345678"), null, "country code with trunk zero is rejected");
  eq(normalizeMobile("011-123-4567"), "01101234567", "old 10-digit");
  eq(normalizeMobile("02-123-4567"), null, "landline");
  eq(maskMobile("01012345678"), "010-****-5678", "mask");
});

test("order numbers follow the channel format", () => {
  eq(formatOrderNo("direct", "2026-10-07", 42), "O261007-00042", "direct");
  eq(formatOrderNo("market", "2026-10-07", 123, "GMKT"), "M-GMKT-261007-000123", "market");
});

test("payload round-trips and strips control characters", () => {
  const raw = encodePayload({ orderNo: "O261007-00042\t", customerId: "C1001", kind: "SHIPPED" });
  eq(raw.includes("\t"), false, "no tab on the wire");
  eq(decodePayload(raw), { orderNo: "O261007-00042", customerId: "C1001", kind: "SHIPPED" }, "decode");
  eq(decodePayload("{"), null, "garbage");
  eq(decodePayload('{"v":9,"o":"x","c":"y","k":"PAID"}'), null, "unknown version");
});

test("alert text fits the sms width", () => {
  const t = renderAlert("DELIVERED", "김하늘", "M-GMKT-261007-000123");
  eq(displayWidth(t) <= 80, true, "width");
  eq(clip("가나다라", 5), "가나…", "clip");
});

run();
