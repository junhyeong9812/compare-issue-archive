export interface Clock {
  now(): number;
}

export class ManualClock implements Clock {
  constructor(private t = 0) {}
  now(): number {
    return this.t;
  }
  advance(ms: number): void {
    if (ms < 0) throw new Error("clock cannot go backwards");
    this.t += ms;
  }
}
