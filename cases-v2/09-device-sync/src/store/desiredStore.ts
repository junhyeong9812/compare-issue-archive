import { decodeProfile, encodeProfile } from "../codec/settingsCodec";
import { DeviceProfile } from "../domain/profile";

/** The configuration operators want each device to run. */
export class DesiredStore {
  private readonly rows = new Map<string, string>();

  set(profile: DeviceProfile): void {
    this.rows.set(profile.deviceId, encodeProfile(profile));
  }

  get(deviceId: string): DeviceProfile | undefined {
    const body = this.rows.get(deviceId);
    return body === undefined ? undefined : decodeProfile(body);
  }
}
