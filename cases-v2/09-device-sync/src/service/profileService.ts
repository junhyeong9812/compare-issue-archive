import { SyncConfig } from "../config";
import { DeviceProfile } from "../domain/profile";
import { Revision, RevisionSource } from "../domain/revision";
import { RevisionStore } from "../store/revisionStore";
import { Clock } from "../util/clock";
import { valuesEqual } from "../util/deepEqual";
import { changedKeys } from "../util/diff";
import { normalizeProfile } from "./normalize";

export interface ApplyResult {
  changed: boolean;
  revision: Revision;
}

function withoutIgnored(p: DeviceProfile, ignored: readonly string[]): DeviceProfile {
  const settings = { ...p.settings };
  for (const k of ignored) delete settings[k];
  return { ...p, settings };
}

/** Records a device configuration; a new revision is created only when something actually changed. */
export class ProfileService {
  constructor(private readonly revisions: RevisionStore, private readonly clock: Clock, private readonly config: SyncConfig) {}

  apply(incoming: DeviceProfile, source: RevisionSource): ApplyResult {
    const next = withoutIgnored(normalizeProfile(incoming, this.config.decimals), this.config.ignoredKeys);
    const latest = this.revisions.latest(next.deviceId);
    if (latest && this.sameProfile(latest.profile, next)) {
      return { changed: false, revision: latest };
    }
    const changed = latest ? changedKeys(latest.profile.settings, next.settings) : Object.keys(next.settings).sort();
    const revision = this.revisions.append(next.deviceId, next, source, this.clock.now(), changed);
    return { changed: true, revision };
  }

  private sameProfile(a: DeviceProfile, b: DeviceProfile): boolean {
    return a.name === b.name && a.firmware === b.firmware && valuesEqual(a.settings, b.settings);
  }
}
