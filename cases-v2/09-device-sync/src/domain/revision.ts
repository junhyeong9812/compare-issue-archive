import { DeviceProfile } from "./profile";

export type RevisionSource = "report" | "import";

export interface Revision {
  deviceId: string;
  rev: number;
  createdAt: number;
  source: RevisionSource;
  profile: DeviceProfile;
  /** Top-level setting keys that differ from the previous revision. */
  changed: string[];
}
