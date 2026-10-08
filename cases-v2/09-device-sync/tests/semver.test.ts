import { compareVersions, needsUpgrade, newest } from "../src/util/semver";
import { eq, run, test } from "./harness";

test("versions compare numerically, not as text", () => {
  eq(compareVersions("1.10.0", "1.9.9") > 0, true, "minor 10 > 9");
  eq(compareVersions("2.0.0", "2.0.0"), 0, "equal");
  eq(needsUpgrade("3.1.9", "3.2.0"), true, "below minimum");
  eq(needsUpgrade("3.2.0", "3.2.0"), false, "at minimum");
});

test("newest skips malformed entries", () => {
  eq(newest(["1.2.3", "beta", "1.10.0", "1.9.0"]), "1.10.0", "newest");
  eq(newest([]), undefined, "empty");
});

run();
