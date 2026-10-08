import { roundTo } from "../codec/numberFormat";
import { DeviceProfile, SettingValue, isPlainObject } from "../domain/profile";

function normalizeValue(v: SettingValue, decimals: number): SettingValue {
  if (typeof v === "number") return roundTo(v, decimals);
  if (typeof v === "string") return v.trim();
  if (Array.isArray(v)) return v.map((x) => normalizeValue(x, decimals));
  if (isPlainObject(v)) {
    const out: { [k: string]: SettingValue } = {};
    for (const k of Object.keys(v).sort()) out[k] = normalizeValue(v[k] as SettingValue, decimals);
    return out;
  }
  return v;
}

/** Canonical form used for storage and comparison: trimmed strings, rounded numbers, sorted keys. */
export function normalizeProfile(p: DeviceProfile, decimals: number): DeviceProfile {
  return {
    deviceId: p.deviceId.trim(),
    name: p.name.trim(),
    firmware: p.firmware.trim(),
    settings: normalizeValue(p.settings, decimals) as Record<string, SettingValue>,
  };
}
