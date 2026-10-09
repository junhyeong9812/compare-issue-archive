import { Autopay } from "../src/domain/autopay";
import { reminderKey } from "../src/domain/reminder";
import { maskDevice, normalizeDevice } from "../src/push/deviceToken";
import { renderMessage } from "../src/push/messageTemplates";
import { shortMerchant, won } from "../src/util/money";
import { eq, run, test } from "./harness";

const contract: Autopay = {
  id: "AP00007",
  memberId: "M1",
  merchant: "아주긴이름의프리미엄스트리밍서비스",
  amount: 1234567,
  billingDay: 5,
  billingTime: "09:30",
  active: true,
};

test("money and merchant names are formatted for one push line", () => {
  eq(won(0), "0원", "zero");
  eq(won(1234567), "1,234,567원", "thousands");
  eq(won(-5000), "-5,000원", "negative");
  eq(shortMerchant("든든손해보험"), "든든손해보험", "short name");
  eq(shortMerchant(contract.merchant), "아주긴이름의프리미엄스…", "long name");
});

test("reminder texts mention merchant, amount and withdrawal time", () => {
  eq(
    renderMessage("BILL_DUE", contract, "2026-11-05T09:30:00"),
    "[한빛페이] 아주긴이름의프리미엄스… 자동이체 1,234,567원이 11/5 09:30에 출금됩니다. 잔액을 확인해 주세요.",
    "bill due",
  );
  eq(
    renderMessage("RETRY", contract, "2026-11-05T11:30:12"),
    "[한빛페이] 아주긴이름의프리미엄스… 자동이체 1,234,567원 출금이 실패해 11/5 11:30에 다시 출금합니다.",
    "retry",
  );
});

test("reminder keys and device tokens are stable", () => {
  eq(reminderKey("BILL_DUE", "AP00007", "2026-11-05T09:30:00"), "rmd.AP00007.due.20261105", "due key");
  eq(reminderKey("RETRY", "AP00007", "2026-11-05T11:30:12"), "rmd.AP00007.retry.20261105", "retry key");
  eq(normalizeDevice("  IOS:AbCdEf123456 "), "ios:AbCdEf123456", "normalized");
  eq(maskDevice("and:Zz9988776655"), "and:Zz99…55", "masked");
  let threw = false;
  try {
    normalizeDevice("web:AbCdEf123456");
  } catch {
    threw = true;
  }
  eq(threw, true, "unknown platform rejected");
});

run();
