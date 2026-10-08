export interface Clock {
  now(): number;
}

export interface DraftStore {
  /** Persists the snapshot. Throws when the write fails. */
  write(text: string): void;
}

export type Listener = (text: string) => void;

function fingerprint(text: string): string {
  let h = 2166136261;
  for (let i = 0; i < text.length; i++) {
    h ^= text.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return `${text.length}:${(h >>> 0).toString(16)}`;
}

/**
 * Watches an editor buffer. Called once per poll tick by the host loop.
 * Pushes every change to listeners immediately (live preview) and writes
 * the draft to storage at most once per `saveIntervalMs`.
 */
export class DraftAutosaver {
  private lastFp: string | null = null;
  private pending: string | null = null;
  private dirty = false;
  private lastSave = Number.NEGATIVE_INFINITY;
  private readonly listeners: Listener[] = [];

  constructor(
    private readonly readBuffer: () => string,
    private readonly store: DraftStore,
    private readonly clock: Clock,
    private readonly saveIntervalMs = 1000,
  ) {}

  onChange(listener: Listener): void {
    this.listeners.push(listener);
  }

  tick(): void {
    const text = this.readBuffer();
    const fp = fingerprint(text);
    if (fp === this.lastFp) return;

    this.lastFp = fp;
    this.pending = text;
    this.dirty = true;
    for (const l of this.listeners) l(text);

    const now = this.clock.now();
    if (this.dirty && now - this.lastSave >= this.saveIntervalMs) {
      try {
        this.store.write(this.pending);
      } catch {
        // storage errors are transient; next save will pick it up
      }
      this.dirty = false;
      this.lastSave = now;
    }
  }
}
