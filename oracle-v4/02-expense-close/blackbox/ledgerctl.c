/*
 * ledgerctl — mock of the in-house ledger vendor CLI (Hanbit Pay, experiment v3).
 *
 * Undocumented rule (the case's black box):
 *   `post` accepts any valid memo and always prints a receipt with exit 0,
 *   but when the memo is longer than 48 bytes (raw UTF-8 bytes, not characters)
 *   the entry is stored with status 4 (held) instead of 1 (posted).
 *   `list` only prints status-1 entries, so held entries silently disappear
 *   from every query. `show` prints the status as an opaque number.
 *
 * Errors are opaque codes on stderr only (E11..E31). Deterministic: no clock,
 * no randomness; state lives in $LEDGERCTL_HOME/journal.dat.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/file.h>

#define MAXLINE 4096

static const char *USAGE =
    "usage: ledgerctl <command> [options]\n"
    "       ledgerctl --version\n"
    "\n"
    "commands:\n"
    "  post  --acct CODE --amt N --date YYYY-MM-DD --ref REF --memo TEXT\n"
    "        prints the receipt number\n"
    "  list  --from YYYY-MM-DD --to YYYY-MM-DD\n"
    "        receipt, date, acct, amt, ref, memo (tab separated)\n"
    "  show  RECEIPT\n"
    "\n"
    "environment:\n"
    "  LEDGERCTL_HOME   journal directory (required)\n";

static void die(int code, int rc) {
    fprintf(stderr, "E%02d\n", code);
    exit(rc);
}

static const char *journal_path(void) {
    static char buf[1024];
    const char *home = getenv("LEDGERCTL_HOME");
    if (!home || !*home) die(10, 2);
    if (snprintf(buf, sizeof buf, "%s/journal.dat", home) >= (int)sizeof buf) die(10, 2);
    return buf;
}

static int valid_date(const char *s) {
    if (!s || strlen(s) != 10) return 0;
    for (int i = 0; i < 10; i++) {
        if (i == 4 || i == 7) { if (s[i] != '-') return 0; }
        else if (s[i] < '0' || s[i] > '9') return 0;
    }
    int m = (s[5] - '0') * 10 + (s[6] - '0');
    int d = (s[8] - '0') * 10 + (s[9] - '0');
    return m >= 1 && m <= 12 && d >= 1 && d <= 31;
}

static int valid_code(const char *s, size_t maxlen, int allow_dash) {
    size_t n = s ? strlen(s) : 0;
    if (n == 0 || n > maxlen) return 0;
    for (size_t i = 0; i < n; i++) {
        unsigned char c = (unsigned char)s[i];
        int ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
        if (allow_dash && (c == '-' || c == '_')) ok = 1;
        if (!ok) return 0;
    }
    return 1;
}

/* well-formed UTF-8, no control characters */
static int valid_text(const unsigned char *s) {
    size_t i = 0;
    if (!s[0]) return 0;
    while (s[i]) {
        unsigned char c = s[i];
        int extra;
        if (c < 0x20 || c == 0x7f) return 0;
        if (c < 0x80) { i++; continue; }
        else if ((c & 0xE0) == 0xC0 && c >= 0xC2) extra = 1;
        else if ((c & 0xF0) == 0xE0) extra = 2;
        else if ((c & 0xF8) == 0xF0 && c <= 0xF4) extra = 3;
        else return 0;
        for (int k = 1; k <= extra; k++)
            if ((s[i + k] & 0xC0) != 0x80) return 0;
        i += extra + 1;
    }
    return 1;
}

static void hex_encode(const char *in, char *out) {
    static const char hx[] = "0123456789abcdef";
    size_t j = 0;
    for (size_t i = 0; in[i]; i++) {
        unsigned char c = (unsigned char)in[i];
        out[j++] = hx[c >> 4];
        out[j++] = hx[c & 15];
    }
    out[j] = 0;
}

static int hexval(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    return -1;
}

static void hex_decode(const char *in, char *out) {
    size_t j = 0;
    for (size_t i = 0; in[i] && in[i + 1]; i += 2) {
        int a = hexval(in[i]), b = hexval(in[i + 1]);
        if (a < 0 || b < 0) break;
        out[j++] = (char)(a * 16 + b);
    }
    out[j] = 0;
}

typedef struct {
    long seq;
    int st;
    char date[16];
    char acct[32];
    long long amt;
    char ref[64];
    char memo[MAXLINE];
} rec_t;

static int parse_rec(char *line, rec_t *r) {
    char *f[7];
    int n = 0;
    char *p = line;
    size_t len = strlen(line);
    if (len && line[len - 1] == '\n') line[len - 1] = 0;
    while (n < 7) {
        f[n++] = p;
        char *bar = strchr(p, '|');
        if (!bar) break;
        *bar = 0;
        p = bar + 1;
    }
    if (n != 7) return 0;
    r->seq = atol(f[0]);
    r->st = atoi(f[1]);
    snprintf(r->date, sizeof r->date, "%s", f[2]);
    snprintf(r->acct, sizeof r->acct, "%s", f[3]);
    r->amt = atoll(f[4]);
    snprintf(r->ref, sizeof r->ref, "%s", f[5]);
    hex_decode(f[6], r->memo);
    return 1;
}

static const char *opt(int argc, char **argv, const char *name) {
    for (int i = 0; i + 1 < argc; i++)
        if (strcmp(argv[i], name) == 0) return argv[i + 1];
    return NULL;
}

static int classify(const char *memo) {
    /* status for a new entry */
    size_t limit = (size_t)(6 << 3);
    return strlen(memo) > limit ? 4 : 1;
}

static int cmd_post(int argc, char **argv) {
    const char *acct = opt(argc, argv, "--acct");
    const char *amt = opt(argc, argv, "--amt");
    const char *date = opt(argc, argv, "--date");
    const char *ref = opt(argc, argv, "--ref");
    const char *memo = opt(argc, argv, "--memo");
    if (!acct || !amt || !date || !ref || !memo) die(11, 2);
    if (!valid_code(acct, 10, 0)) die(14, 3);
    char *end;
    long long a = strtoll(amt, &end, 10);
    if (*end || a <= 0 || a > 999999999999LL) die(12, 3);
    if (!valid_date(date)) die(13, 3);
    if (!valid_code(ref, 20, 1)) die(16, 3);
    if (strlen(memo) > 1000 || !valid_text((const unsigned char *)memo)) die(17, 3);

    const char *path = journal_path();
    int fd = open(path, O_RDWR | O_CREAT, 0644);
    if (fd < 0) die(21, 4);
    if (flock(fd, LOCK_EX) != 0) die(21, 4);
    FILE *fp = fdopen(fd, "r+");
    if (!fp) die(21, 4);
    char line[MAXLINE * 2 + 256];
    long seq = 0;
    while (fgets(line, sizeof line, fp)) seq++;
    seq++;
    char hex[MAXLINE * 2];
    hex_encode(memo, hex);
    fseek(fp, 0, SEEK_END);
    fprintf(fp, "%ld|%d|%s|%s|%lld|%s|%s\n", seq, classify(memo), date, acct, a, ref, hex);
    if (fflush(fp) != 0) die(21, 4);
    fclose(fp);
    printf("R%08ld\n", seq);
    return 0;
}

static int cmd_list(int argc, char **argv) {
    const char *from = opt(argc, argv, "--from");
    const char *to = opt(argc, argv, "--to");
    if (!from || !to) die(11, 2);
    if (!valid_date(from) || !valid_date(to)) die(13, 3);
    FILE *fp = fopen(journal_path(), "r");
    if (!fp) return 0;
    char line[MAXLINE * 2 + 256];
    rec_t r;
    while (fgets(line, sizeof line, fp)) {
        if (!parse_rec(line, &r)) continue;
        if (r.st != 1) continue;
        if (strcmp(r.date, from) < 0 || strcmp(r.date, to) > 0) continue;
        printf("R%08ld\t%s\t%s\t%lld\t%s\t%s\n", r.seq, r.date, r.acct, r.amt, r.ref, r.memo);
    }
    fclose(fp);
    return 0;
}

static int cmd_show(int argc, char **argv) {
    if (argc < 1) die(11, 2);
    const char *rc = argv[0];
    if (rc[0] != 'R' || strlen(rc) != 9) die(31, 3);
    long want = atol(rc + 1);
    FILE *fp = fopen(journal_path(), "r");
    if (!fp) die(31, 3);
    char line[MAXLINE * 2 + 256];
    rec_t r;
    while (fgets(line, sizeof line, fp)) {
        if (!parse_rec(line, &r) || r.seq != want) continue;
        printf("receipt R%08ld\ndate %s\nacct %s\namt %lld\nref %s\nst %d\nmemo %s\n",
               r.seq, r.date, r.acct, r.amt, r.ref, r.st, r.memo);
        fclose(fp);
        return 0;
    }
    fclose(fp);
    die(31, 3);
    return 3;
}

int main(int argc, char **argv) {
    if (argc < 2 || strcmp(argv[1], "--help") == 0 || strcmp(argv[1], "-h") == 0) {
        fputs(USAGE, argc < 2 ? stderr : stdout);
        return argc < 2 ? 2 : 0;
    }
    if (strcmp(argv[1], "--version") == 0) { puts("ledgerctl 5.2.0"); return 0; }
    const char *cmd = argv[1];
    if (strcmp(cmd, "post") == 0) return cmd_post(argc - 2, argv + 2);
    if (strcmp(cmd, "list") == 0) return cmd_list(argc - 2, argv + 2);
    if (strcmp(cmd, "show") == 0) return cmd_show(argc - 2, argv + 2);
    die(11, 2);
    return 2;
}
