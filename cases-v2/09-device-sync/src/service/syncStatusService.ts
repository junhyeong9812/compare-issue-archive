import { SyncConfig } from "../config";
import { DesiredStore } from "../store/desiredStore";
import { RevisionStore } from "../store/revisionStore";
import { changedKeys } from "../util/diff";
import { needsUpgrade } from "../util/semver";

export interface SyncStatus {
  deviceId: string;
  state: "unknown" | "in-sync" | "drifted";
  reportedRev: number | null;
  drift: string[];
  firmwareMismatch: boolean;
  upgradeRecommended: boolean;
}

export class SyncStatusService {
  constructor(private readonly desired: DesiredStore, private readonly revisions: RevisionStore, private readonly config: SyncConfig) {}

  status(deviceId: string): SyncStatus {
    const want = this.desired.get(deviceId);
    const have = this.revisions.latest(deviceId);
    if (!want || !have) {
      return {
        deviceId,
        state: "unknown",
        reportedRev: have?.rev ?? null,
        drift: [],
        firmwareMismatch: false,
        upgradeRecommended: have ? needsUpgrade(have.profile.firmware, this.config.minimumFirmware) : false,
      };
    }
    const drift = changedKeys(want.settings, have.profile.settings, this.config.ignoredKeys);
    const firmwareMismatch = want.firmware !== have.profile.firmware;
    return {
      deviceId,
      state: drift.length === 0 && !firmwareMismatch ? "in-sync" : "drifted",
      reportedRev: have.rev,
      drift,
      firmwareMismatch,
      upgradeRecommended: needsUpgrade(have.profile.firmware, this.config.minimumFirmware),
    };
  }
}
