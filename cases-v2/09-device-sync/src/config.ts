export interface SyncConfig {
  /** Numeric settings are stored with this many decimal places. */
  decimals: number;
  /** How many revisions the history endpoint returns (newest first). */
  historyLimit: number;
  /** Keys that never take part in drift detection (device-local bookkeeping). */
  ignoredKeys: string[];
  /** Devices below this firmware are flagged on the status page. */
  minimumFirmware: string;
}

export const defaultConfig: SyncConfig = {
  decimals: 6,
  historyLimit: 50,
  ignoredKeys: ["uptimeSec", "lastBootAt"],
  minimumFirmware: "3.0.0",
};
