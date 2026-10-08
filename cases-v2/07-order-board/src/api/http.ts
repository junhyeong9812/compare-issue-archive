export interface Request {
  method: "GET" | "POST";
  path: string;
  query: Record<string, string | undefined>;
  body?: unknown;
}

export interface Response {
  status: number;
  body: unknown;
}

export const ok = (body: unknown): Response => ({ status: 200, body });
export const badRequest = (message: string): Response => ({ status: 400, body: { error: message } });
export const notFound = (message: string): Response => ({ status: 404, body: { error: `not found: ${message}` } });

export function parseUrl(url: string): { path: string; query: Record<string, string> } {
  const [path, qs = ""] = url.split("?");
  const query: Record<string, string> = {};
  for (const part of qs.split("&")) {
    if (!part) continue;
    const [k, v = ""] = part.split("=");
    query[decodeURIComponent(k)] = decodeURIComponent(v);
  }
  return { path, query };
}
