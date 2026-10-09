import { spawnSync } from "child_process";

export interface WireMessage {
  key: string;
  payload: string;
}

export interface PublishResult {
  accepted: number;
  batch: number;
}

export interface ConsumedMessage {
  offset: number;
  key: string;
  payload: string;
}

export class MqpubError extends Error {
  constructor(readonly code: string, readonly exitStatus: number | null) {
    super(`mqpub ${code} (exit ${exitStatus})`);
  }
}

/** 사내 메시지 발행 CLI(mqpub 3.x) 래퍼 */
export class MqpubClient {
  constructor(private readonly bin: string, private readonly spoolDir: string) {}

  private run(args: string[], input?: string): string {
    const r = spawnSync(this.bin, args, {
      input,
      encoding: "utf8",
      env: { ...process.env, MQPUB_SPOOL: this.spoolDir },
    });
    if (r.error) throw r.error;
    if (r.status !== 0) {
      const code = /E\d+/.exec(r.stderr)?.[0] ?? "E?";
      throw new MqpubError(code, r.status);
    }
    return r.stdout;
  }

  /** 메시지들을 한 배치로 발행한다. */
  publish(topic: string, messages: WireMessage[]): PublishResult {
    if (messages.length === 0) return { accepted: 0, batch: 0 };
    const body = messages.map((m) => `${m.key}\t${m.payload}`).join("\n") + "\n";
    const out = this.run(["publish", "-t", topic], body);
    const m = /^accepted (\d+) batch (\d+)$/m.exec(out);
    if (!m) throw new MqpubError("unparsed-output", 0);
    return { accepted: Number(m[1]), batch: Number(m[2]) };
  }

  /** offset 부터 끝까지 읽는다. */
  consume(topic: string, fromOffset: number): ConsumedMessage[] {
    const out = this.run(["consume", "-t", topic, "-o", String(fromOffset)]);
    const rows: ConsumedMessage[] = [];
    for (const line of out.split("\n")) {
      if (line === "") continue;
      const [off, key, ...rest] = line.split("\t");
      rows.push({ offset: Number(off), key, payload: rest.join("\t") });
    }
    return rows;
  }
}
