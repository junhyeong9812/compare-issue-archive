/* pushq — 앱 푸시 예약 벤더 CLI 모의(블랙박스).
 * 숨은 규칙: schedule 의 --at 이 하루 기준 300초 경계(분이 5의 배수이고 초가 00)가 아니면
 * 티켓 번호는 정상 발급하고 exit 0 이지만 큐에는 넣지 않는다. 그 티켓은 list 에 없고,
 * run 이 시각을 지나가도 log 에 나오지 않으며, cancel 하면 E31.
 * 규칙을 드러내는 문자열은 넣지 않는다(오류는 E 코드만).
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAXQ 4000
#define DEVMAX 64
#define TAGMAX 64
#define MSGMAX 512

static long long unit(void) { return (long long)(1 << 2) * 75; } /* 상수 문자열로 남기지 않는다 */

static void die(int code, int rc) {
  fprintf(stderr, "pushq: E%d\n", code);
  exit(rc);
}

static void usage(FILE *f) {
  fputs("usage:\n"
        "  pushq schedule --to <device> --at <YYYY-MM-DDTHH:MM:SS> [--tag <tag>] <message>\n"
        "  pushq send --to <device> [--tag <tag>] <message>\n"
        "  pushq list [--to <device>]          print <ticket>TAB<at>TAB<device>TAB<tag>\n"
        "  pushq run --until <YYYY-MM-DDTHH:MM:SS>\n"
        "  pushq log [--to <device>]           print <at>TAB<device>TAB<tag>TAB<message>\n"
        "  pushq cancel <ticket>\n"
        "limits: device 1..64, tag 0..64, message 1..512 bytes\n"
        "env: PUSHQ_HOME (state directory, required)\n",
        f);
}

/* ---- 시각 ---- */
static long long days_from_civil(int y, int m, int d) {
  y -= m <= 2;
  long long era = (y >= 0 ? y : y - 399) / 400;
  long long yoe = y - era * 400;
  long long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
  long long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
  return era * 146097 + doe - 719468;
}

static void civil_from_days(long long z, int *y, int *m, int *d) {
  z += 719468;
  long long era = (z >= 0 ? z : z - 146096) / 146097;
  long long doe = z - era * 146097;
  long long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
  long long yy = yoe + era * 400;
  long long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
  long long mp = (5 * doy + 2) / 153;
  int dd = (int)(doy - (153 * mp + 2) / 5 + 1);
  int mm = (int)(mp < 10 ? mp + 3 : mp - 9);
  *y = (int)(yy + (mm <= 2)); *m = mm; *d = dd;
}

static int dim(int y, int m) {
  static const int t[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
  if (m == 2 && ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0)) return 29;
  return t[m - 1];
}

static int num(const char *s, int n) {
  int v = 0;
  for (int i = 0; i < n; i++) {
    if (s[i] < '0' || s[i] > '9') return -1;
    v = v * 10 + (s[i] - '0');
  }
  return v;
}

static int parse_at(const char *s, long long *out) {
  if (strlen(s) != 19 || s[4] != '-' || s[7] != '-' || s[10] != 'T' || s[13] != ':' || s[16] != ':') return -1;
  int y = num(s, 4), mo = num(s + 5, 2), d = num(s + 8, 2), h = num(s + 11, 2), mi = num(s + 14, 2), se = num(s + 17, 2);
  if (y < 1970 || mo < 1 || mo > 12 || d < 1 || h < 0 || h > 23 || mi < 0 || mi > 59 || se < 0 || se > 59) return -1;
  if (d > dim(y, mo)) return -1;
  *out = days_from_civil(y, mo, d) * 86400LL + h * 3600LL + mi * 60LL + se;
  return 0;
}

static void fmt_at(long long t, char *buf, size_t n) {
  long long days = t / 86400, r = t % 86400;
  int y, m, d;
  civil_from_days(days, &y, &m, &d);
  snprintf(buf, n, "%04d-%02d-%02dT%02d:%02d:%02d", y, m, d, (int)(r / 3600), (int)(r % 3600 / 60), (int)(r % 60));
}

/* ---- 인코딩 ---- */
static void hexenc(const char *s, char *out) {
  static const char *dg = "0123456789abcdef";
  size_t n = strlen(s);
  if (n == 0) { strcpy(out, "-"); return; }
  for (size_t i = 0; i < n; i++) { out[2*i] = dg[((unsigned char)s[i]) >> 4]; out[2*i+1] = dg[((unsigned char)s[i]) & 15]; }
  out[2*n] = 0;
}

static int hv(char c) {
  if (c >= '0' && c <= '9') return c - '0';
  if (c >= 'a' && c <= 'f') return c - 'a' + 10;
  return -1;
}

static int hexdec(const char *h, char *out) {
  if (strcmp(h, "-") == 0) { out[0] = 0; return 0; }
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

static unsigned int mix(const char *s, unsigned int h) {
  for (; *s; s++) { h ^= (unsigned char)*s; h *= 16777619u; }
  return h;
}

static unsigned int chk(long a, long long b, const char *x, const char *y, const char *z) {
  char buf[64];
  snprintf(buf, sizeof buf, "%ld/%lld", a, b);
  unsigned int h = 2166136261u ^ 0x2f6a91c3u;
  h = mix(buf, h); h = mix("|", h); h = mix(x, h); h = mix("|", h); h = mix(y, h); h = mix("|", h); h = mix(z, h);
  return h;
}

/* ---- 상태 ---- */
static const char *home(void) {
  const char *s = getenv("PUSHQ_HOME");
  if (!s || !*s) die(21, 2);
  return s;
}

static void path(char *buf, size_t n, const char *ext) { snprintf(buf, n, "%s/pushq.%s", home(), ext); }

static long long read_ll(const char *ext, long long dflt) {
  char p[1024]; path(p, sizeof p, ext);
  FILE *f = fopen(p, "r");
  if (!f) return dflt;
  long long v;
  if (fscanf(f, "%lld", &v) != 1) v = dflt;
  fclose(f);
  return v;
}

static void write_ll(const char *ext, long long v) {
  char p[1024], t[1040]; path(p, sizeof p, ext);
  snprintf(t, sizeof t, "%s.tmp", p);
  FILE *f = fopen(t, "w");
  if (!f) die(31, 3);
  fprintf(f, "%lld\n", v);
  if (fclose(f) != 0 || rename(t, p) != 0) die(31, 3);
}

typedef struct { long ticket; long long at; char dev[2*DEVMAX+2]; char tag[2*TAGMAX+2]; char msg[2*MSGMAX+2]; } item_t;

static item_t q[MAXQ];
static int qn;

static void load_q(void) {
  char p[1024]; path(p, sizeof p, "q");
  qn = 0;
  FILE *f = fopen(p, "r");
  if (!f) return;
  static char line[4 * (DEVMAX + TAGMAX + MSGMAX) + 128];
  while (fgets(line, sizeof line, f)) {
    if (qn >= MAXQ) die(34, 3);
    item_t *it = &q[qn];
    unsigned int c;
    if (sscanf(line, "%ld %lld %129s %129s %1025s %x", &it->ticket, &it->at, it->dev, it->tag, it->msg, &c) != 6) die(33, 3);
    if (c != chk(it->ticket, it->at, it->dev, it->tag, it->msg)) die(33, 3);
    qn++;
  }
  fclose(f);
}

static void save_q(void) {
  char p[1024], t[1040]; path(p, sizeof p, "q");
  snprintf(t, sizeof t, "%s.tmp", p);
  FILE *f = fopen(t, "w");
  if (!f) die(31, 3);
  for (int i = 0; i < qn; i++)
    fprintf(f, "%ld %lld %s %s %s %08x\n", q[i].ticket, q[i].at, q[i].dev, q[i].tag, q[i].msg,
            chk(q[i].ticket, q[i].at, q[i].dev, q[i].tag, q[i].msg));
  if (fclose(f) != 0 || rename(t, p) != 0) die(31, 3);
}

static void append_log(long long at, const char *hdev, const char *htag, const char *hmsg) {
  char p[1024]; path(p, sizeof p, "log");
  FILE *f = fopen(p, "a");
  if (!f) die(31, 3);
  fprintf(f, "%lld %s %s %s %08x\n", at, hdev, htag, hmsg, chk(0, at, hdev, htag, hmsg));
  if (fclose(f) != 0) die(31, 3);
}

/* ---- 입력 검사 ---- */
static int ident_ok(const char *s, size_t lo, size_t hi) {
  size_t n = strlen(s);
  if (n < lo || n > hi) return 0;
  for (size_t i = 0; i < n; i++) {
    char c = s[i];
    if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ':' || c == '.' || c == '_' || c == '-')) return 0;
  }
  return 1;
}

static int msg_ok(const char *s) {
  size_t n = strlen(s);
  if (n < 1 || n > MSGMAX) return 0;
  for (size_t i = 0; i < n; i++) if ((unsigned char)s[i] < 0x20) return 0;
  return 1;
}

/* ---- 명령 ---- */
typedef struct { const char *to, *at, *tag, *until, *pos; } args_t;

static void parse_args(int argc, char **argv, args_t *a) {
  memset(a, 0, sizeof *a);
  for (int i = 2; i < argc; i++) {
    if (!strcmp(argv[i], "--to") && i + 1 < argc) a->to = argv[++i];
    else if (!strcmp(argv[i], "--at") && i + 1 < argc) a->at = argv[++i];
    else if (!strcmp(argv[i], "--tag") && i + 1 < argc) a->tag = argv[++i];
    else if (!strcmp(argv[i], "--until") && i + 1 < argc) a->until = argv[++i];
    else if (argv[i][0] == '-' && argv[i][1] == '-') die(10, 2);
    else if (!a->pos) a->pos = argv[i];
    else die(10, 2);
  }
}

static int cmd_schedule(const args_t *a) {
  if (!a->to || !a->at || !a->pos) die(10, 2);
  if (!ident_ok(a->to, 1, DEVMAX)) die(12, 2);
  if (a->tag && !ident_ok(a->tag, 0, TAGMAX)) die(16, 2);
  if (!msg_ok(a->pos)) die(15, 2);
  long long at;
  if (parse_at(a->at, &at) != 0) die(14, 2);
  long long clk = read_ll("clk", -1);
  if (clk >= 0 && at <= clk) die(23, 2);
  load_q();
  if (qn >= MAXQ) die(34, 3);
  long ticket = (long)read_ll("seq", 0) + 1;
  if (at % unit() == 0) {
    item_t *it = &q[qn++];
    it->ticket = ticket; it->at = at;
    hexenc(a->to, it->dev); hexenc(a->tag ? a->tag : "", it->tag); hexenc(a->pos, it->msg);
    save_q();
  }
  write_ll("seq", ticket);
  printf("ticket T%06ld\n", ticket);
  return 0;
}

static int cmd_send(const args_t *a) {
  if (!a->to || !a->pos || a->at) die(10, 2);
  if (!ident_ok(a->to, 1, DEVMAX)) die(12, 2);
  if (a->tag && !ident_ok(a->tag, 0, TAGMAX)) die(16, 2);
  if (!msg_ok(a->pos)) die(15, 2);
  long long clk = read_ll("clk", -1);
  if (clk < 0) die(25, 2);
  char hd[2*DEVMAX+2], ht[2*TAGMAX+2], hm[2*MSGMAX+2];
  hexenc(a->to, hd); hexenc(a->tag ? a->tag : "", ht); hexenc(a->pos, hm);
  append_log(clk, hd, ht, hm);
  puts("sent 1");
  return 0;
}

static int cmp_item(const void *x, const void *y) {
  const item_t *a = x, *b = y;
  if (a->at != b->at) return a->at < b->at ? -1 : 1;
  return a->ticket < b->ticket ? -1 : (a->ticket > b->ticket);
}

static int cmd_run(const args_t *a) {
  if (!a->until || a->pos) die(10, 2);
  long long until;
  if (parse_at(a->until, &until) != 0) die(14, 2);
  long long clk = read_ll("clk", -1);
  if (clk >= 0 && until < clk) die(23, 2);
  load_q();
  qsort(q, (size_t)qn, sizeof q[0], cmp_item);
  int kept = 0, done = 0;
  for (int i = 0; i < qn; i++) {
    if (q[i].at <= until) { append_log(q[i].at, q[i].dev, q[i].tag, q[i].msg); done++; }
    else q[kept++] = q[i];
  }
  qn = kept;
  save_q();
  write_ll("clk", until);
  printf("delivered %d\n", done);
  return 0;
}

static int cmd_list(const args_t *a) {
  if (a->pos || a->at || a->until) die(10, 2);
  load_q();
  qsort(q, (size_t)qn, sizeof q[0], cmp_item);
  char hd[2*DEVMAX+2] = "";
  if (a->to) { if (!ident_ok(a->to, 1, DEVMAX)) die(12, 2); hexenc(a->to, hd); }
  static char dev[DEVMAX + 2], tag[TAGMAX + 2];
  char ts[32];
  for (int i = 0; i < qn; i++) {
    if (a->to && strcmp(hd, q[i].dev) != 0) continue;
    if (hexdec(q[i].dev, dev) < 1 || hexdec(q[i].tag, tag) < 0) die(33, 3);
    fmt_at(q[i].at, ts, sizeof ts);
    printf("T%06ld\t%s\t%s\t%s\n", q[i].ticket, ts, dev, tag);
  }
  return 0;
}

static int cmd_log(const args_t *a) {
  if (a->pos || a->at || a->until) die(10, 2);
  char hd[2*DEVMAX+2] = "";
  if (a->to) { if (!ident_ok(a->to, 1, DEVMAX)) die(12, 2); hexenc(a->to, hd); }
  char p[1024]; path(p, sizeof p, "log");
  FILE *f = fopen(p, "r");
  if (!f) return 0;
  static char line[4 * (DEVMAX + TAGMAX + MSGMAX) + 128];
  static char d[2*DEVMAX+2], t[2*TAGMAX+2], m[2*MSGMAX+2];
  static char dev[DEVMAX + 2], tag[TAGMAX + 2], msg[MSGMAX + 2];
  char ts[32];
  while (fgets(line, sizeof line, f)) {
    long long at; unsigned int c;
    if (sscanf(line, "%lld %129s %129s %1025s %x", &at, d, t, m, &c) != 5) die(33, 3);
    if (c != chk(0, at, d, t, m)) die(33, 3);
    if (a->to && strcmp(hd, d) != 0) continue;
    if (hexdec(d, dev) < 1 || hexdec(t, tag) < 0 || hexdec(m, msg) < 1) die(33, 3);
    fmt_at(at, ts, sizeof ts);
    printf("%s\t%s\t%s\t%s\n", ts, dev, tag, msg);
  }
  fclose(f);
  return 0;
}

static int cmd_cancel(const args_t *a) {
  if (!a->pos || a->to || a->at || a->until) die(10, 2);
  const char *s = a->pos;
  if (s[0] != 'T' || strlen(s) < 2) die(13, 2);
  char *e; long t = strtol(s + 1, &e, 10);
  if (*e || t <= 0) die(13, 2);
  load_q();
  int found = -1;
  for (int i = 0; i < qn; i++) if (q[i].ticket == t) { found = i; break; }
  if (found < 0) die(31, 2);
  for (int i = found; i + 1 < qn; i++) q[i] = q[i + 1];
  qn--;
  save_q();
  printf("cancelled T%06ld\n", t);
  return 0;
}

int main(int argc, char **argv) {
  if (argc < 2) { usage(stderr); die(10, 2); }
  const char *cmd = argv[1];
  if (!strcmp(cmd, "--help") || !strcmp(cmd, "-h") || !strcmp(cmd, "help")) { usage(stdout); return 0; }
  if (!strcmp(cmd, "--version")) { puts("pushq 2.8.0"); return 0; }
  args_t a;
  parse_args(argc, argv, &a);
  home();
  if (!strcmp(cmd, "schedule")) return cmd_schedule(&a);
  if (!strcmp(cmd, "send")) return cmd_send(&a);
  if (!strcmp(cmd, "run")) return cmd_run(&a);
  if (!strcmp(cmd, "list")) return cmd_list(&a);
  if (!strcmp(cmd, "log")) return cmd_log(&a);
  if (!strcmp(cmd, "cancel")) return cmd_cancel(&a);
  die(10, 2);
  return 2;
}
