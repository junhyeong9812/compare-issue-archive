export type SettingValue = number | string | boolean | null | SettingValue[] | { [key: string]: SettingValue };

export interface DeviceProfile {
  deviceId: string;
  name: string;
  firmware: string;
  settings: Record<string, SettingValue>;
}

export function isPlainObject(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null && !Array.isArray(v);
}

export function validateProfile(p: unknown): string[] {
  const errors: string[] = [];
  if (!isPlainObject(p)) return ["profile must be an object"];
  if (typeof p.deviceId !== "string" || !p.deviceId) errors.push("deviceId is required");
  if (typeof p.name !== "string") errors.push("name must be a string");
  if (typeof p.firmware !== "string" || !/^\d+\.\d+\.\d+$/.test(p.firmware)) errors.push("firmware must look like 1.2.3");
  if (!isPlainObject(p.settings)) errors.push("settings must be an object");
  return errors;
}
