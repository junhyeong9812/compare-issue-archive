/**
 * 앱이 올려 주는 기기 토큰을 벤더가 받는 형태로 정리한다.
 * 앱 구버전은 앞뒤 공백·대문자 플랫폼 접두("IOS:")를 섞어 보낸다.
 */
const PLATFORMS = ["ios", "and"];

export function normalizeDevice(raw: string): string {
  const t = raw.trim();
  const i = t.indexOf(":");
  if (i <= 0) throw new Error("device token needs platform prefix");
  const platform = t.slice(0, i).toLowerCase();
  const body = t.slice(i + 1);
  if (!PLATFORMS.includes(platform)) throw new Error(`unknown platform ${platform}`);
  if (!/^[A-Za-z0-9_-]{8,48}$/.test(body)) throw new Error("device token body malformed");
  return `${platform}:${body}`;
}

export function maskDevice(device: string): string {
  const [p, body] = device.split(":");
  return `${p}:${body.slice(0, 4)}…${body.slice(-2)}`;
}
