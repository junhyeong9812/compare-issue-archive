export interface Page<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
  hasNext: boolean;
}

export function paginate<T>(all: T[], page: number, pageSize: number): Page<T> {
  const size = Math.max(1, Math.floor(pageSize));
  const p = Math.max(1, Math.floor(page));
  const start = (p - 1) * size;
  const items = all.slice(start, start + size);
  return { items, page: p, pageSize: size, total: all.length, hasNext: start + size < all.length };
}

/** Stable multi-key sort. Each key returns a number or string; ties fall through to the next key. */
export function sortBy<T>(items: T[], ...keys: ((t: T) => number | string)[]): T[] {
  return items
    .map((item, index) => ({ item, index }))
    .sort((a, b) => {
      for (const key of keys) {
        const ka = key(a.item);
        const kb = key(b.item);
        if (ka < kb) return -1;
        if (ka > kb) return 1;
      }
      return a.index - b.index;
    })
    .map((x) => x.item);
}
