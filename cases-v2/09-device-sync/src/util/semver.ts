export interface Version {
  major: number;
  minor: number;
  patch: number;
}

export function parseVersion(s: string): Version {
  const m = /^(\d+)\.(\d+)\.(\d+)$/.exec(s.trim());
  if (!m) throw new Error(`not a version: ${s}`);
  return { major: Number(m[1]), minor: Number(m[2]), patch: Number(m[3]) };
}

export function compareVersions(a: string, b: string): number {
  const x = parseVersion(a);
  const y = parseVersion(b);
  return x.major - y.major || x.minor - y.minor || x.patch - y.patch;
}

/** Devices below the minimum firmware get a rollout recommendation instead of config pushes. */
export function needsUpgrade(current: string, minimum: string): boolean {
  return compareVersions(current, minimum) < 0;
}

/** Picks the newest version from a list, ignoring malformed entries. */
export function newest(versions: string[]): string | undefined {
  let best: string | undefined;
  for (const v of versions) {
    try {
      parseVersion(v);
    } catch {
      continue;
    }
    if (best === undefined || compareVersions(v, best) > 0) best = v;
  }
  return best;
}
