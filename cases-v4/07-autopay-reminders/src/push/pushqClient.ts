import { spawnSync } from "child_process";
import { LocalDateTime } from "../time/localTime";

export interface QueuedPush {
  ticket: string;
  at: LocalDateTime;
  device: string;
  tag: string;
}

export interface DeliveredPush {
  at: LocalDateTime;
  device: string;
  tag: string;
  message: string;
}

export class PushqError extends Error {
  constructor(readonly code: string, readonly exitStatus: number | null) {
    super(`pushq ${code} (exit ${exitStatus})`);
  }
}

/** 앱 푸시 예약 벤더 CLI(pushq 2.x) 래퍼 */
export class PushqClient {
  constructor(private readonly bin: string, private readonly home: string) {}

  private run(args: string[]): string {
    const r = spawnSync(this.bin, args, {
      encoding: "utf8",
      env: { ...process.env, PUSHQ_HOME: this.home },
    });
    if (r.error) throw r.error;
    if (r.status !== 0) {
      const code = /E\d+/.exec(r.stderr)?.[0] ?? "E?";
      throw new PushqError(code, r.status);
    }
    return r.stdout;
  }

  /** 지정 시각 발송을 예약하고 티켓 번호를 돌려준다. */
  schedule(device: string, at: LocalDateTime, tag: string, message: string): string {
    const out = this.run(["schedule", "--to", device, "--at", at, "--tag", tag, message]);
    const m = /^ticket (T\d+)$/m.exec(out);
    if (!m) throw new PushqError("unparsed-output", 0);
    return m[1];
  }

  cancel(ticket: string): void {
    this.run(["cancel", ticket]);
  }

  /** 벤더 시계를 until 까지 진행시켜 그 사이 예약분을 내보낸다. */
  advance(until: LocalDateTime): number {
    const out = this.run(["run", "--until", until]);
    const m = /^delivered (\d+)$/m.exec(out);
    if (!m) throw new PushqError("unparsed-output", 0);
    return Number(m[1]);
  }

  queued(device?: string): QueuedPush[] {
    const out = this.run(device ? ["list", "--to", device] : ["list"]);
    return rows(out).map(([ticket, at, dev, tag]) => ({ ticket, at, device: dev, tag }));
  }

  delivered(device?: string): DeliveredPush[] {
    const out = this.run(device ? ["log", "--to", device] : ["log"]);
    return rows(out).map(([at, dev, tag, ...msg]) => ({ at, device: dev, tag, message: msg.join("\t") }));
  }
}

function rows(out: string): string[][] {
  return out
    .split("\n")
    .filter((l) => l !== "")
    .map((l) => l.split("\t"));
}
