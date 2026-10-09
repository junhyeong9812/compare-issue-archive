export interface Response {
  status: number;
  body: unknown;
}

export type Handler = (params: Record<string, string>, body: unknown) => Response;

interface Route {
  method: string;
  parts: string[];
  handler: Handler;
}

/** 아주 작은 경로 라우터 — 테스트와 운영 어댑터가 같은 진입점을 쓴다. */
export class Router {
  private readonly routes: Route[] = [];

  add(method: string, pattern: string, handler: Handler): this {
    this.routes.push({ method, parts: pattern.split("/").filter(Boolean), handler });
    return this;
  }

  handle(method: string, url: string, body?: unknown): Response {
    const segs = url.split("?")[0].split("/").filter(Boolean).map(decodeURIComponent);
    for (const r of this.routes) {
      if (r.method !== method || r.parts.length !== segs.length) continue;
      const params: Record<string, string> = {};
      const matched = r.parts.every((p, i) => {
        if (p.startsWith(":")) {
          params[p.slice(1)] = segs[i];
          return true;
        }
        return p === segs[i];
      });
      if (!matched) continue;
      try {
        return r.handler(params, body);
      } catch (e) {
        return { status: 400, body: { error: e instanceof Error ? e.message : String(e) } };
      }
    }
    return { status: 404, body: { error: "not found" } };
  }
}

export const ok = (body: unknown): Response => ({ status: 200, body });
export const notFound = (what: string): Response => ({ status: 404, body: { error: `${what} not found` } });
