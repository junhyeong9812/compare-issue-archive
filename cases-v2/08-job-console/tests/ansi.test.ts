import { collapseCarriageReturns, stripAnsi } from "../src/util/ansi";
import { ScriptedProcess } from "../src/runtime/process";
import { OutputTail } from "../src/runtime/tail";
import { eq, run, test } from "./harness";

test("color codes are removed", () => {
  eq(stripAnsi("\x1b[32mPASS\x1b[0m unit"), "PASS unit", "sgr");
  eq(stripAnsi("\x1b]8;;http://x\x07link\x1b]8;;\x07"), "link", "osc hyperlink");
});

test("progress bars keep only the final redraw", () => {
  eq(collapseCarriageReturns("10%\r55%\r100%"), "100%", "cr");
  eq(collapseCarriageReturns("plain"), "plain", "no cr");
});

test("tail splits chunks into display lines", () => {
  let t = 0;
  const p = new ScriptedProcess(
    [
      { at: 0, write: "comp" },
      { at: 0, write: "iling...\n\x1b[1mok\x1b[0m\n" },
      { at: 5, write: "late\n" },
    ],
    () => t,
  );
  const tail = new OutputTail(p);
  eq(tail.read(), ["compiling...", "ok"], "at 0");
  t = 5;
  eq(tail.read(), ["compiling...", "ok", "late"], "at 5");
});

run();
