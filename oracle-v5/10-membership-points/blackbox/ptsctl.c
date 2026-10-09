/*
 * ptsctl — mock of the points vendor CLI (Hanbit Pay, experiment v3).
 *
 * Undocumented rule (the case's black box):
 *   `earn --mbr X` reads an all-digit member number as a NUMBER: leading
 *   zeros are dropped, so "000123" is credited to account "123" (another
 *   member, or a new orphan account). Exit 0, an entry number is printed.
 *   Only the vendor notation "@X" keeps X verbatim. Member numbers with any
 *   non-digit character are always kept verbatim.
 *   `bal` and `hist` look accounts up by the exact text given ("@" is
 *   stripped, nothing else), so the misplaced points are visible only there.
 *
 * Errors are opaque codes on stderr only. Deterministic: no clock, no
 * randomness; state lives in $PTSCTL_HOME/book.dat.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/file.h>

#define MAXL 512

static const char *USAGE =
    "usage: ptsctl <command> [options]\n"
    "\n"
    "commands:\n"
    "  earn  --mbr MBR --pt N --ref REF\n"
    "        prints the entry number\n"
    "  bal   MBR\n"
    "  hist  MBR\n"
    "        entry, ref, points (tab separated)\n"
    "\n"
    "  MBR   member number; @MBR is also accepted\n"
    "  REF   [A-Za-z0-9-], 1..32 chars, once per ledger\n"
    "\n"
    "  --version   prints the version\n"
    "\n"
    "environment:\n"
    "  PTSCTL_HOME   ledger directory (required)\n";

static void die(int code, int rc) {
    fprintf(stderr, "E%02d\n", code);
    exit(rc);
}

static char book[1024];

static int alnum_dash(const char *s, size_t maxlen) {
    size_t n = s ? strlen(s) : 0;
    if (n == 0 || n > maxlen) return 0;
    for (size_t i = 0; i < n; i++) {
        unsigned char c = (unsigned char)s[i];
        int ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '-';
        if (!ok) return 0;
    }
    return 1;
}

static int all_digits(const char *s) {
    if (!*s) return 0;
    for (; *s; s++) if (*s < '0' || *s > '9') return 0;
    return 1;
}

/* account key for earn */
static void earn_key(const char *in, char *out, size_t n) {
    if (in[0] == '@') {
        if (!alnum_dash(in + 1, 20)) die(14, 3);
        snprintf(out, n, "%s", in + 1);
        return;
    }
    if (!alnum_dash(in, 20)) die(14, 3);
    if (all_digits(in)) {
        unsigned long long v = strtoull(in, NULL, 10);
        snprintf(out, n, "%llu", v);
        return;
    }
    snprintf(out, n, "%s", in);
}

/* account key for lookups */
static void look_key(const char *in, char *out, size_t n) {
    const char *s = in[0] == '@' ? in + 1 : in;
    if (!alnum_dash(s, 20)) die(14, 3);
    snprintf(out, n, "%s", s);
}

static const char *opt(int argc, char **argv, const char *name) {
    for (int i = 0; i + 1 < argc; i++)
        if (strcmp(argv[i], name) == 0) return argv[i + 1];
    return NULL;
}

/* book.dat: seq|acct|ref|pt */
typedef struct { long seq; char acct[32]; char ref[48]; long long pt; } ent_t;

static int next_ent(FILE *fp, ent_t *e) {
    char line[MAXL];
    while (fgets(line, sizeof line, fp)) {
        size_t n = strlen(line);
        if (n && line[n - 1] == '\n') line[n - 1] = 0;
        char *f[4];
        int k = 0;
        char *p = line;
        while (k < 4) {
            f[k++] = p;
            char *bar = strchr(p, '|');
            if (!bar) break;
            *bar = 0;
            p = bar + 1;
        }
        if (k != 4) continue;
        e->seq = atol(f[0]);
        snprintf(e->acct, sizeof e->acct, "%s", f[1]);
        snprintf(e->ref, sizeof e->ref, "%s", f[2]);
        e->pt = atoll(f[3]);
        return 1;
    }
    return 0;
}

static int cmd_earn(int argc, char **argv) {
    const char *mbr = opt(argc, argv, "--mbr");
    const char *pt = opt(argc, argv, "--pt");
    const char *ref = opt(argc, argv, "--ref");
    if (!mbr || !pt || !ref) die(11, 2);
    char key[32];
    earn_key(mbr, key, sizeof key);
    if (!all_digits(pt) || strlen(pt) > 9) die(12, 3);
    long long p = atoll(pt);
    if (p <= 0) die(12, 3);
    if (!alnum_dash(ref, 32)) die(16, 3);

    int fd = open(book, O_RDWR | O_CREAT, 0644);
    if (fd < 0 || flock(fd, LOCK_EX) != 0) die(21, 4);
    FILE *fp = fdopen(fd, "r+");
    if (!fp) die(21, 4);
    ent_t e;
    long seq = 0;
    while (next_ent(fp, &e)) {
        seq = e.seq;
        if (strcmp(e.ref, ref) == 0) die(19, 3);
    }
    seq++;
    fseek(fp, 0, SEEK_END);
    fprintf(fp, "%ld|%s|%s|%lld\n", seq, key, ref, p);
    if (fflush(fp) != 0) die(21, 4);
    fclose(fp);
    printf("P%07ld\n", seq);
    return 0;
}

static int cmd_bal(int argc, char **argv, int hist) {
    if (argc < 1) die(11, 2);
    char key[32];
    look_key(argv[0], key, sizeof key);
    FILE *fp = fopen(book, "r");
    long long sum = 0;
    if (fp) {
        ent_t e;
        while (next_ent(fp, &e)) {
            if (strcmp(e.acct, key) != 0) continue;
            sum += e.pt;
            if (hist) printf("P%07ld\t%s\t%lld\n", e.seq, e.ref, e.pt);
        }
        fclose(fp);
    }
    if (!hist) printf("%s %lld\n", key, sum);
    return 0;
}

int main(int argc, char **argv) {
    if (argc < 2 || strcmp(argv[1], "--help") == 0 || strcmp(argv[1], "-h") == 0) {
        fputs(USAGE, argc < 2 ? stderr : stdout);
        return argc < 2 ? 2 : 0;
    }
    if (strcmp(argv[1], "--version") == 0) {
        puts("ptsctl 4.2.0");
        return 0;
    }
    const char *h = getenv("PTSCTL_HOME");
    if (!h || !*h) die(10, 2);
    if (snprintf(book, sizeof book, "%s/book.dat", h) >= (int)sizeof book) die(10, 2);
    const char *cmd = argv[1];
    if (strcmp(cmd, "earn") == 0) return cmd_earn(argc - 2, argv + 2);
    if (strcmp(cmd, "bal") == 0) return cmd_bal(argc - 2, argv + 2, 0);
    if (strcmp(cmd, "hist") == 0) return cmd_bal(argc - 2, argv + 2, 1);
    die(11, 2);
    return 2;
}
