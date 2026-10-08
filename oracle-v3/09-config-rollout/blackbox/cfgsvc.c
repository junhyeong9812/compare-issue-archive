/*
 * cfgsvc — mock of the in-house configuration service vendor CLI
 * (Hanbit Pay, experiment v3).
 *
 * Undocumented rule (the case's black box):
 *   `set` always records the change and prints a change number with exit 0,
 *   but `get` keeps answering with the value as of the last `mark` for keys
 *   that already had a value then. A change only becomes visible to `get`
 *   after `mark` (documented merely as "prints the current mark").
 *   Keys that had no value at the last `mark` answer with their newest change
 *   right away. `keys` shows each key's newest change and an opaque st
 *   (0 = visible, 3 = waiting for the next mark).
 *
 * Errors are opaque codes on stderr only (E10..E31). Deterministic: no clock,
 * no randomness; state lives in $CFGSVC_HOME (journal.dat + mark.dat).
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/file.h>

#define MAXLINE 2048
#define MAXREC 8192

static const char *USAGE =
    "usage: cfgsvc <command> [args]\n"
    "\n"
    "commands:\n"
    "  set   KEY VALUE    prints the change number\n"
    "  get   KEY          prints the value\n"
    "  keys  [PREFIX]     key, change, st (tab separated)\n"
    "  mark               prints the current mark\n"
    "\n"
    "environment:\n"
    "  CFGSVC_HOME   state directory (required)\n";

static void die(int code, int rc) {
    fprintf(stderr, "E%02d\n", code);
    exit(rc);
}

static void path_of(char *buf, size_t n, const char *leaf) {
    const char *h = getenv("CFGSVC_HOME");
    if (!h || !*h) die(10, 2);
    if (snprintf(buf, n, "%s/%s", h, leaf) >= (int)n) die(10, 2);
}

static int valid_key(const char *s) {
    size_t n = s ? strlen(s) : 0;
    if (n == 0 || n > 64 || s[0] == '.' || s[n - 1] == '.') return 0;
    for (size_t i = 0; i < n; i++) {
        unsigned char c = (unsigned char)s[i];
        int ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || c == '.' || c == '-' || c == 0x5f;
        if (!ok) return 0;
    }
    return 1;
}

/* well-formed UTF-8, no control characters, at most 512 bytes */
static int valid_value(const unsigned char *s) {
    size_t i = 0;
    if (strlen((const char *)s) > 512) return 0;
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
    long chg;
    int st;
    char key[80];
    char val[600];
} rec_t;

static rec_t recs[MAXREC];
static int nrec;

static int parse_rec(char *line, rec_t *r) {
    char *f[4];
    int n = 0;
    char *p = line;
    size_t len = strlen(line);
    if (len && line[len - 1] == '\n') line[len - 1] = 0;
    while (n < 4) {
        f[n++] = p;
        char *bar = strchr(p, '|');
        if (!bar) break;
        *bar = 0;
        p = bar + 1;
    }
    if (n != 4) return 0;
    r->chg = atol(f[0]);
    r->st = atoi(f[1]);
    snprintf(r->key, sizeof r->key, "%s", f[2]);
    hex_decode(f[3], r->val);
    return 1;
}

static void load(FILE *fp) {
    char line[MAXLINE];
    nrec = 0;
    rewind(fp);
    while (nrec < MAXREC && fgets(line, sizeof line, fp))
        if (parse_rec(line, &recs[nrec])) nrec++;
}

static FILE *open_journal(int lock) {
    char p[1024];
    path_of(p, sizeof p, "journal.dat");
    int fd = open(p, O_RDWR | O_CREAT, 0644);
    if (fd < 0) die(21, 4);
    if (flock(fd, lock) != 0) die(21, 4);
    FILE *fp = fdopen(fd, "r+");
    if (!fp) die(21, 4);
    return fp;
}

static long read_mark(void) {
    char p[1024];
    path_of(p, sizeof p, "mark.dat");
    FILE *fp = fopen(p, "r");
    if (!fp) return 0;
    long m = 0;
    if (fscanf(fp, "%ld", &m) != 1) m = 0;
    fclose(fp);
    return m;
}

static void write_mark(long m) {
    char p[1024], t[1100];
    path_of(p, sizeof p, "mark.dat");
    snprintf(t, sizeof t, "%s.tmp", p);
    FILE *fp = fopen(t, "w");
    if (!fp) die(21, 4);
    fprintf(fp, "%ld\n", m);
    if (fclose(fp) != 0 || rename(t, p) != 0) die(21, 4);
}

static int cmd_set(int argc, char **argv) {
    if (argc != 2) die(11, 2);
    if (!valid_key(argv[0])) die(14, 3);
    if (!valid_value((const unsigned char *)argv[1])) die(17, 3);
    FILE *fp = open_journal(LOCK_EX);
    load(fp);
    long chg = nrec + 1;
    char hex[1100];
    hex_encode(argv[1], hex);
    fseek(fp, 0, SEEK_END);
    fprintf(fp, "%ld|%d|%s|%s\n", chg, 3, argv[0], hex);
    if (fflush(fp) != 0) die(21, 4);
    fclose(fp);
    printf("C%07ld\n", chg);
    return 0;
}

static int cmd_get(int argc, char **argv) {
    if (argc != 1) die(11, 2);
    if (!valid_key(argv[0])) die(14, 3);
    FILE *fp = open_journal(LOCK_SH);
    load(fp);
    fclose(fp);
    int seen = -1, waiting = -1;
    for (int i = 0; i < nrec; i++) {
        if (strcmp(recs[i].key, argv[0]) != 0) continue;
        if (recs[i].st == 0) seen = i;
        else waiting = i;
    }
    int pick = seen >= 0 ? seen : waiting;
    if (pick < 0) die(31, 3);
    printf("%s\n", recs[pick].val);
    return 0;
}

static int cmp_key(const void *a, const void *b) {
    const rec_t *x = *(rec_t * const *)a, *y = *(rec_t * const *)b;
    return strcmp(x->key, y->key);
}

static int cmd_keys(int argc, char **argv) {
    const char *prefix = argc >= 1 ? argv[0] : "";
    FILE *fp = open_journal(LOCK_SH);
    load(fp);
    fclose(fp);
    static rec_t *latest[MAXREC];
    int n = 0;
    for (int i = 0; i < nrec; i++) {
        if (strncmp(recs[i].key, prefix, strlen(prefix)) != 0) continue;
        int j;
        for (j = 0; j < n; j++)
            if (strcmp(latest[j]->key, recs[i].key) == 0) break;
        if (j < n) latest[j] = &recs[i];
        else latest[n++] = &recs[i];
    }
    qsort(latest, n, sizeof latest[0], cmp_key);
    for (int i = 0; i < n; i++)
        printf("%s\tC%07ld\t%d\n", latest[i]->key, latest[i]->chg, latest[i]->st);
    return 0;
}

static int cmd_mark(void) {
    FILE *fp = open_journal(LOCK_EX);
    load(fp);
    long m = read_mark();
    int changed = 0;
    for (int i = 0; i < nrec; i++)
        if (recs[i].st != 0) { recs[i].st = 0; changed = 1; }
    if (changed) {
        char hex[1100];
        if (ftruncate(fileno(fp), 0) != 0) die(21, 4);
        rewind(fp);
        for (int i = 0; i < nrec; i++) {
            hex_encode(recs[i].val, hex);
            fprintf(fp, "%ld|%d|%s|%s\n", recs[i].chg, recs[i].st, recs[i].key, hex);
        }
        if (fflush(fp) != 0) die(21, 4);
        m++;
        write_mark(m);
    }
    fclose(fp);
    printf("M%07ld\n", m);
    return 0;
}

int main(int argc, char **argv) {
    if (argc < 2 || strcmp(argv[1], "--help") == 0 || strcmp(argv[1], "-h") == 0) {
        fputs(USAGE, argc < 2 ? stderr : stdout);
        return argc < 2 ? 2 : 0;
    }
    const char *cmd = argv[1];
    if (strcmp(cmd, "set") == 0) return cmd_set(argc - 2, argv + 2);
    if (strcmp(cmd, "get") == 0) return cmd_get(argc - 2, argv + 2);
    if (strcmp(cmd, "keys") == 0) return cmd_keys(argc - 2, argv + 2);
    if (strcmp(cmd, "mark") == 0) return cmd_mark();
    die(11, 2);
    return 2;
}
