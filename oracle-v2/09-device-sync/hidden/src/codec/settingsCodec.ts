import { DeviceProfile } from "../domain/profile";

/**
 * Storage encoding for profiles. Plain JSON cannot carry NaN or ±Infinity
 * (JSON.stringify turns them into null), so non-finite numbers are boxed.
 */
const BOX = "$num";

function box(_key: string, value: unknown): unknown {
  if (typeof value === "number" && !Number.isFinite(value)) {
    return { [BOX]: Number.isNaN(value) ? "NaN" : value > 0 ? "Infinity" : "-Infinity" };
  }
  return value;
}

function unbox(_key: string, value: unknown): unknown {
  if (typeof value === "object" && value !== null && !Array.isArray(value)) {
    const keys = Object.keys(value);
    if (keys.length === 1 && keys[0] === BOX) return Number((value as Record<string, string>)[BOX]);
  }
  return value;
}

export function encodeProfile(p: DeviceProfile): string {
  return JSON.stringify(p, box);
}

export function decodeProfile(text: string): DeviceProfile {
  return JSON.parse(text, unbox) as DeviceProfile;
}
