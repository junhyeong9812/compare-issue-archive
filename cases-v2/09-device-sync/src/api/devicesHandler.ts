import { formatNumber } from "../codec/numberFormat";
import { SyncConfig } from "../config";
import { DeviceProfile, SettingValue, validateProfile } from "../domain/profile";
import { Revision } from "../domain/revision";
import { ImportService } from "../service/importService";
import { normalizeProfile } from "../service/normalize";
import { ProfileService } from "../service/profileService";
import { SyncStatusService } from "../service/syncStatusService";
import { DesiredStore } from "../store/desiredStore";
import { RevisionStore } from "../store/revisionStore";

export interface Response {
  status: number;
  body: unknown;
}

const bad = (errors: string[]): Response => ({ status: 400, body: { errors } });

function display(v: SettingValue): string {
  if (typeof v === "number") return formatNumber(v);
  if (Array.isArray(v)) return `[${v.map(display).join(", ")}]`;
  if (v !== null && typeof v === "object") return `{${Object.keys(v).map((k) => `${k}: ${display(v[k])}`).join(", ")}}`;
  return String(v);
}

export class DevicesHandler {
  constructor(
    private readonly profiles: ProfileService,
    private readonly revisions: RevisionStore,
    private readonly desired: DesiredStore,
    private readonly sync: SyncStatusService,
    private readonly importer: ImportService,
    private readonly config: SyncConfig,
  ) {}

  /** Heartbeat from a device agent carrying its current configuration. */
  report(deviceId: string, body: unknown): Response {
    const errors = validateProfile(body);
    if (errors.length) return bad(errors);
    const p = body as DeviceProfile;
    if (p.deviceId !== deviceId) return bad(["deviceId does not match path"]);
    const res = this.profiles.apply(p, "report");
    return { status: res.changed ? 201 : 200, body: { rev: res.revision.rev, changed: res.changed } };
  }

  history(deviceId: string): Response {
    const items = this.revisions.history(deviceId, this.config.historyLimit).map((r: Revision) => ({
      rev: r.rev,
      source: r.source,
      changed: r.changed,
      settings: r.profile.settings,
      summary: Object.keys(r.profile.settings).map((k) => `${k}=${display(r.profile.settings[k])}`).join("; "),
    }));
    return { status: 200, body: { deviceId, count: this.revisions.count(deviceId), items } };
  }

  setDesired(deviceId: string, body: unknown): Response {
    const errors = validateProfile(body);
    if (errors.length) return bad(errors);
    const p = body as DeviceProfile;
    if (p.deviceId !== deviceId) return bad(["deviceId does not match path"]);
    this.desired.set(normalizeProfile(p, this.config.decimals));
    return { status: 204, body: null };
  }

  status(deviceId: string): Response {
    return { status: 200, body: this.sync.status(deviceId) };
  }

  import(body: unknown): Response {
    if (!Array.isArray(body)) return bad(["expected an array of profiles"]);
    return { status: 200, body: this.importer.importAll(body) };
  }
}
