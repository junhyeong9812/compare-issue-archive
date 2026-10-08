// Node 18 런타임에서 쓰는 최소 선언(@types/node 미설치 환경).
declare const process: {
  env: Record<string, string | undefined>;
  exitCode?: number;
  cwd(): string;
};

declare module "child_process" {
  export interface SpawnSyncReturns {
    status: number | null;
    stdout: string;
    stderr: string;
    error?: Error;
  }
  export function spawnSync(
    command: string,
    args: string[],
    options: { input?: string; encoding: "utf8"; env?: Record<string, string | undefined> },
  ): SpawnSyncReturns;
}

declare module "crypto" {
  export interface Hash {
    update(data: string): Hash;
    digest(encoding: "hex"): string;
  }
  export function createHash(algorithm: string): Hash;
}

declare module "fs" {
  export function mkdtempSync(base: string): string;
  export function rmSync(path: string, options: { recursive: boolean; force: boolean }): void;
  export function existsSync(path: string): boolean;
}

declare module "path" {
  export function join(...parts: string[]): string;
}

declare module "os" {
  export function tmpdir(): string;
}
