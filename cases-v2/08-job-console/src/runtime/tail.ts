import { collapseCarriageReturns, stripAnsi } from "../util/ansi";
import { ProcessHandle } from "./process";

/** Turns raw process output into display lines. */
export class OutputTail {
  constructor(private readonly proc: ProcessHandle) {}

  read(): string[] {
    const text = this.proc.output().join("");
    const lines = text.split("\n");
    if (lines[lines.length - 1] === "") lines.pop();
    return lines.map((l) => collapseCarriageReturns(stripAnsi(l)));
  }
}
