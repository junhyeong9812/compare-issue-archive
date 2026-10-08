import { decodeProfile, encodeProfile } from "../codec/settingsCodec";
import { DeviceProfile } from "../domain/profile";
import { Revision, RevisionSource } from "../domain/revision";

interface Row {
  deviceId: string;
  rev: number;
  createdAt: number;
  source: RevisionSource;
  body: string;
  changed: string;
}

/** Append-only revision log, stored the way the database table holds it (encoded text). */
export class RevisionStore {
  private readonly rows: Row[] = [];
  private readonly latestIndex = new Map<string, number>();

  append(deviceId: string, profile: DeviceProfile, source: RevisionSource, createdAt: number, changed: string[]): Revision {
    const prev = this.latest(deviceId);
    const row: Row = {
      deviceId,
      rev: prev ? prev.rev + 1 : 1,
      createdAt,
      source,
      body: encodeProfile(profile),
      changed: changed.join(","),
    };
    this.rows.push(row);
    this.latestIndex.set(deviceId, this.rows.length - 1);
    return this.toRevision(row);
  }

  latest(deviceId: string): Revision | undefined {
    const i = this.latestIndex.get(deviceId);
    return i === undefined ? undefined : this.toRevision(this.rows[i]);
  }

  /** Newest first. */
  history(deviceId: string, limit: number): Revision[] {
    return this.rows
      .filter((r) => r.deviceId === deviceId)
      .sort((a, b) => b.rev - a.rev)
      .slice(0, limit)
      .map((r) => this.toRevision(r));
  }

  count(deviceId: string): number {
    return this.rows.filter((r) => r.deviceId === deviceId).length;
  }

  private toRevision(r: Row): Revision {
    return {
      deviceId: r.deviceId,
      rev: r.rev,
      createdAt: r.createdAt,
      source: r.source,
      profile: decodeProfile(r.body),
      changed: r.changed ? r.changed.split(",") : [],
    };
  }
}
