export interface AppConfig {
  pageSize: number;
  locale: string;
  /** Orders older than this (ms) in PENDING are flagged as stale on the dashboard. */
  staleAfterMs: number;
}

export const defaultConfig: AppConfig = {
  pageSize: 20,
  locale: "ko-KR",
  staleAfterMs: 24 * 60 * 60 * 1000,
};
