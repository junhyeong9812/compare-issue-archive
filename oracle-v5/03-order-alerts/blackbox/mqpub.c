/* mqpub — 사내 메시지 발행 벤더 CLI 모의(블랙박스).
 * 숨은 규칙: 한 번의 publish(배치) 안에서 키의 앞 16바이트가 같은 메시지는
 * 첫 것만 저장하고 나머지는 오류 없이 버린다. 출력의 accepted 는 요청 건수 그대로.
 * 규칙을 드러내는 문자열은 넣지 않는다(오류는 E 코드만).
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAXLINE 4096
#define MAXBATCH 500
#define KEYMAX 64
#define PAYMAX 1024

static int kw(void) { return (KEYMAX >> 2); } /* 비교 폭 — 상수 문자열로 남기지 않는다 */

static void die(int code, int rc) {
  fprintf(stderr, "mqpub: E%d\n", code);
  exit(rc);
}

static void usage(FILE *f) {
  fputs("usage:\n"
        "  mqpub publish -t <topic> [-f <file>]   read lines <key>TAB<payload>, publish as one batch\n"
        "  mqpub consume -t <topic> [-o <offset>] print <offset>TAB<key>TAB<payload>\n"
        "  mqpub stat -t <topic>\n"
        "limits: key 1..64 bytes, payload 0..1024 bytes, 500 lines per batch\n"
        "env: MQPUB_SPOOL (spool directory, required)\n",
        f);
}

static int topic_ok(const char *t) {
  size_t n = strlen(t);
  if (n == 0 || n > 32) return 0;
  for (size_t i = 0; i < n; i++) {
    char c = t[i];
    if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-')) return 0;
  }
  return 1;
}

static unsigned int mix(const char *s, unsigned int h) {
  for (; *s; s++) { h ^= (unsigned char)*s; h *= 16777619u; }
  return h;
}

static unsigned int chk(long batch, const char *hk, const char *hp) {
  char b[32];
  snprintf(b, sizeof b, "%ld", batch);
  unsigned int h = 2166136261u ^ 0x5bd1e995u;
  h = mix(b, h); h = mix("|", h); h = mix(hk, h); h = mix("|", h); h = mix(hp, h);
  return h;
}

static void hexenc(const char *s, size_t n, char *out) {
  static const char *d = "0123456789abcdef";
  for (size_t i = 0; i < n; i++) { out[2*i] = d[((unsigned char)s[i]) >> 4]; out[2*i+1] = d[((unsigned char)s[i]) & 15]; }
  out[2*n] = 0;
}

static int hv(char c) {
  if (c >= '0' && c <= '9') return c - '0';
  if (c >= 'a' && c <= 'f') return c - 'a' + 10;
  return -1;
}

static int hexdec(const char *h, char *out) {
  size_t n = strlen(h);
  if (n % 2) return -1;
  for (size_t i = 0; i < n / 2; i++) {
    int a = hv(h[2*i]), b = hv(h[2*i+1]);
    if (a < 0 || b < 0) return -1;
    out[i] = (char)(a * 16 + b);
  }
  out[n/2] = 0;
  return (int)(n / 2);
}

static const char *spool(void) {
  const char *s = getenv("MQPUB_SPOOL");
  if (!s || !*s) die(21, 2);
  return s;
}

static void path(char *buf, size_t n, const char *topic, const char *ext) {
  snprintf(buf, n, "%s/%s.%s", spool(), topic, ext);
}

typedef struct { char key[KEYMAX + 1]; size_t klen; char pay[PAYMAX + 1]; size_t plen; } msg_t;

static msg_t batch[MAXBATCH];

static int same_head(const msg_t *a, const msg_t *b) {
  size_t w = (size_t)kw();
  size_t la = a->klen < w ? a->klen : w;
  size_t lb = b->klen < w ? b->klen : w;
  return la == lb && memcmp(a->key, b->key, la) == 0;
}

static int cmd_publish(const char *topic, const char *file) {
  FILE *in = stdin;
  if (file) { in = fopen(file, "r"); if (!in) die(31, 3); }
  char line[MAXLINE + 2];
  int n = 0;
  while (fgets(line, sizeof line, in)) {
    size_t L = strlen(line);
    if (L && line[L-1] == '\n') line[--L] = 0;
    if (L && line[L-1] == '\r') line[--L] = 0;
    if (L == 0) continue;
    char *tab = strchr(line, '\t');
    if (!tab) die(12, 2);
    *tab = 0;
    size_t kl = strlen(line), pl = strlen(tab + 1);
    if (kl == 0 || kl > KEYMAX) die(14, 2);
    if (pl > PAYMAX) die(15, 2);
    if (n >= MAXBATCH) die(16, 2);
    memcpy(batch[n].key, line, kl + 1); batch[n].klen = kl;
    memcpy(batch[n].pay, tab + 1, pl + 1); batch[n].plen = pl;
    n++;
  }
  if (file) fclose(in);
  if (n == 0) die(17, 2);

  char np[1024], qp[1024];
  path(np, sizeof np, topic, "n");
  path(qp, sizeof qp, topic, "q");
  long b = 0;
  FILE *nf = fopen(np, "r");
  if (nf) { if (fscanf(nf, "%ld", &b) != 1) b = 0; fclose(nf); }
  b++;

  FILE *q = fopen(qp, "a");
  if (!q) die(31, 3);
  static char hk[2 * KEYMAX + 1], hp[2 * PAYMAX + 1];
  for (int i = 0; i < n; i++) {
    int skip = 0;
    for (int j = 0; j < i && !skip; j++) if (same_head(&batch[i], &batch[j])) skip = 1;
    if (skip) continue;
    hexenc(batch[i].key, batch[i].klen, hk);
    hexenc(batch[i].pay, batch[i].plen, hp);
    fprintf(q, "%ld %s %s %08x\n", b, hk, hp[0] ? hp : "-", chk(b, hk, hp[0] ? hp : "-"));
  }
  if (fclose(q) != 0) die(31, 3);
  nf = fopen(np, "w");
  if (!nf) die(31, 3);
  fprintf(nf, "%ld\n", b);
  fclose(nf);
  printf("accepted %d batch %ld\n", n, b);
  return 0;
}

static int scan(const char *topic, long from, int print, long *batches, long *count) {
  char qp[1024], np[1024];
  path(qp, sizeof qp, topic, "q");
  path(np, sizeof np, topic, "n");
  *batches = 0; *count = 0;
  FILE *nf = fopen(np, "r");
  if (nf) { if (fscanf(nf, "%ld", batches) != 1) *batches = 0; fclose(nf); }
  FILE *q = fopen(qp, "r");
  if (!q) return 0;
  static char line[2 * (KEYMAX + PAYMAX) + 128];
  static char hk[2 * KEYMAX + 2], hp[2 * PAYMAX + 2];
  static char k[KEYMAX + 2], p[PAYMAX + 2];
  long off = 0;
  while (fgets(line, sizeof line, q)) {
    long b; unsigned int c;
    if (sscanf(line, "%ld %129s %2049s %x", &b, hk, hp, &c) != 4) die(33, 3);
    if (c != chk(b, hk, hp)) die(33, 3);
    if (hexdec(hk, k) < 1) die(33, 3);
    if (strcmp(hp, "-") == 0) p[0] = 0; else if (hexdec(hp, p) < 0) die(33, 3);
    if (print && off >= from) printf("%ld\t%s\t%s\n", off, k, p);
    off++;
  }
  fclose(q);
  *count = off;
  return 0;
}

int main(int argc, char **argv) {
  if (argc < 2) { usage(stderr); die(10, 2); }
  const char *cmd = argv[1];
  if (!strcmp(cmd, "--help") || !strcmp(cmd, "-h") || !strcmp(cmd, "help")) { usage(stdout); return 0; }
  if (!strcmp(cmd, "--version")) { puts("mqpub 3.4.1"); return 0; }
  const char *topic = NULL, *file = NULL; long off = 0;
  for (int i = 2; i < argc; i++) {
    if (!strcmp(argv[i], "-t") && i + 1 < argc) topic = argv[++i];
    else if (!strcmp(argv[i], "-f") && i + 1 < argc) file = argv[++i];
    else if (!strcmp(argv[i], "-o") && i + 1 < argc) {
      char *e; off = strtol(argv[++i], &e, 10);
      if (*e || off < 0) die(10, 2);
    } else die(10, 2);
  }
  if (!topic) die(10, 2);
  if (!topic_ok(topic)) die(11, 2);
  long b, c;
  if (!strcmp(cmd, "publish")) return cmd_publish(topic, file);
  if (!strcmp(cmd, "consume")) { spool(); scan(topic, off, 1, &b, &c); return 0; }
  if (!strcmp(cmd, "stat")) { spool(); scan(topic, 0, 0, &b, &c); printf("topic %s batches %ld messages %ld\n", topic, b, c); return 0; }
  die(10, 2);
  return 2;
}
