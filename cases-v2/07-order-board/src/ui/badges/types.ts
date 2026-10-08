export type Tone = "neutral" | "info" | "success" | "warn" | "danger" | "muted";

export interface Badge {
  label: string;
  tone: Tone;
}
