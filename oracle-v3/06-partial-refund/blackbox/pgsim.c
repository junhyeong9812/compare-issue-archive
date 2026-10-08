/*
 * pgsim — mock of the payment gateway vendor CLI (Hanbit Pay, experiment v3).
 *
 * Undocumented rule (the case's black box):
 *   Inside ONE `refund` call (one batch file), lines that repeat the same
 *   (TID, AMOUNT, AUX) triple — AUX may be empty — are treated as resends of
 *   the first one: only the first is processed, the rest are silently dropped.
 *   The call still prints "accepted <number of lines in the file>" and exits 0.
 *   Lines that differ in AUX are processed separately. Separate calls are not
 *   compared with each other (a repeated batch id is a no-op, also silent).
 *   What was processed is visible only through `status --batch` and `txn`.
 *
 * Errors are opaque codes on stderr only. Deterministic: no clock, no
 * randomness; state lives in $PGSIM_HOME.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/file.h>

#define MAXL 1024
#define MAXITEMS 4096

static const char *USAGE =
    "usage: pgsim <command> [options]\n"
    "\n"
    "commands:\n"
    "  pay     --mid MID --amt N --oid OID\n"
    "          prints the TID\n"
    "  refund  --mid MID --batch BID FILE\n"
    "          FILE: one line per refund: TID<TAB>AMOUNT[<TAB>AUX]\n"
    "          AUX: optional, [A-Za-z0-9-], 1..24 chars\n"
    "          prints \"accepted N\"\n"
    "  status  --batch BID\n"
    "          refund no, tid, amount, aux (tab separated)\n"
    "  txn     TID\n"
    "\n"
    "environment:\n"
    "  PGSIM_HOME   state directory (required)\n";

static void die(int code, int rc) {
    fprintf(stderr, "E%02d\n", code);
    exit(rc);
}

static char home[768];

static void path(char *buf, size_t n, const char *name) {
    if (snprintf(buf, n, "%s/%s", home, name) >= (int)n) die(10, 2);
}

static int lockfd = -1;
static void lock_state(void) {
    char p[1024];
    path(p, sizeof p, ".lk");
    lockfd = open(p, O_RDWR | O_CREAT, 0644);
    if (lockfd < 0 || flock(lockfd, LOCK_EX) != 0) die(20, 4);
}

static int valid_id(const char *s, size_t maxlen) {
    size_t n = s ? strlen(s) : 0;
    if (n == 0 || n > maxlen) return 0;
    for (size_t i = 0; i < n; i++) {
        unsigned char c = (unsigned char)s[i];
        int ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '-';
        if (!ok) return 0;
    }
    return 1;
}

static long long parse_amt(const char *s) {
    if (!s || !*s) return -1;
    for (const char *p = s; *p; p++) if (*p < '0' || *p > '9') return -1;
    if (strlen(s) > 12) return -1;
    long long v = atoll(s);
    return v > 0 ? v : -1;
}

static const char *opt(int argc, char **argv, const char *name) {
    for (int i = 0; i + 1 < argc; i++)
        if (strcmp(argv[i], name) == 0) return argv[i + 1];
    return NULL;
}

static void chomp(char *s) {
    size_t n = strlen(s);
    while (n && (s[n - 1] == '\n' || s[n - 1] == '\r')) s[--n] = 0;
}

/* pay.dat: seq|mid|amt|oid */
typedef struct { long seq; char mid[32]; long long amt; char oid[64]; } pay_t;
/* ref.dat: seq|batch|tidseq|amt|aux */
typedef struct { long seq; char batch[64]; long tid; long long amt; char aux[32]; } ref_t;

static int split(char *line, char **f, int max, char sep) {
    int n = 0;
    char *p = line;
    while (n < max) {
        f[n++] = p;
        char *q = strchr(p, sep);
        if (!q) break;
        *q = 0;
        p = q + 1;
    }
    return n;
}

static int load_pay(long tid, pay_t *out) {
    char p[1024], line[MAXL];
    path(p, sizeof p, "pay.dat");
    FILE *fp = fopen(p, "r");
    if (!fp) return 0;
    while (fgets(line, sizeof line, fp)) {
        chomp(line);
        char *f[4];
        if (split(line, f, 4, '|') != 4) continue;
        if (atol(f[0]) != tid) continue;
        out->seq = tid;
        snprintf(out->mid, sizeof out->mid, "%s", f[1]);
        out->amt = atoll(f[2]);
        snprintf(out->oid, sizeof out->oid, "%s", f[3]);
        fclose(fp);
        return 1;
    }
    fclose(fp);
    return 0;
}

static long count_lines(const char *name) {
    char p[1024], line[MAXL];
    path(p, sizeof p, name);
    FILE *fp = fopen(p, "r");
    if (!fp) return 0;
    long n = 0;
    while (fgets(line, sizeof line, fp)) n++;
    fclose(fp);
    return n;
}

static int each_ref(ref_t *r, FILE *fp) {
    char line[MAXL];
    while (fgets(line, sizeof line, fp)) {
        chomp(line);
        char *f[5];
        if (split(line, f, 5, '|') != 5) continue;
        r->seq = atol(f[0]);
        snprintf(r->batch, sizeof r->batch, "%s", f[1]);
        r->tid = atol(f[2]);
        r->amt = atoll(f[3]);
        snprintf(r->aux, sizeof r->aux, "%s", f[4]);
        return 1;
    }
    return 0;
}

static long long refunded_total(long tid) {
    char p[1024];
    path(p, sizeof p, "ref.dat");
    FILE *fp = fopen(p, "r");
    if (!fp) return 0;
    ref_t r;
    long long sum = 0;
    while (each_ref(&r, fp)) if (r.tid == tid) sum += r.amt;
    fclose(fp);
    return sum;
}

static int batch_seen(const char *bid) {
    char p[1024], line[MAXL];
    path(p, sizeof p, "bat.dat");
    FILE *fp = fopen(p, "r");
    if (!fp) return 0;
    while (fgets(line, sizeof line, fp)) {
        chomp(line);
        char *f[2];
        if (split(line, f, 2, '|') < 1) continue;
        if (strcmp(f[0], bid) == 0) { fclose(fp); return 1; }
    }
    fclose(fp);
    return 0;
}

static long parse_tid(const char *s) {
    if (!s || s[0] != 'T' || strlen(s) != 9) return -1;
    for (int i = 1; i < 9; i++) if (s[i] < '0' || s[i] > '9') return -1;
    return atol(s + 1);
}

static FILE *open_append(const char *name) {
    char p[1024];
    path(p, sizeof p, name);
    FILE *fp = fopen(p, "a");
    if (!fp) die(21, 4);
    return fp;
}

static int cmd_pay(int argc, char **argv) {
    const char *mid = opt(argc, argv, "--mid");
    const char *amt = opt(argc, argv, "--amt");
    const char *oid = opt(argc, argv, "--oid");
    if (!mid || !amt || !oid) die(11, 2);
    if (!valid_id(mid, 16)) die(14, 3);
    long long a = parse_amt(amt);
    if (a < 0) die(12, 3);
    if (!valid_id(oid, 40)) die(16, 3);
    lock_state();
    long seq = count_lines("pay.dat") + 1;
    FILE *fp = open_append("pay.dat");
    fprintf(fp, "%ld|%s|%lld|%s\n", seq, mid, a, oid);
    if (fclose(fp) != 0) die(21, 4);
    printf("T%08ld\n", seq);
    return 0;
}

typedef struct { long tid; long long amt; char aux[32]; int keep; } item_t;

static int cmd_refund(int argc, char **argv) {
    const char *mid = opt(argc, argv, "--mid");
    const char *bid = opt(argc, argv, "--batch");
    const char *file = argc > 0 ? argv[argc - 1] : NULL;
    if (!mid || !bid || !file || file[0] == '-') die(11, 2);
    if (!valid_id(mid, 16)) die(14, 3);
    if (!valid_id(bid, 32)) die(18, 3);
    FILE *in = fopen(file, "r");
    if (!in) die(19, 3);
    static item_t items[MAXITEMS];
    int n = 0;
    char line[MAXL];
    while (fgets(line, sizeof line, in)) {
        chomp(line);
        if (!line[0]) continue;
        if (n >= MAXITEMS) die(15, 3);
        char *f[4];
        int k = split(line, f, 4, '\t');
        if (k < 2 || k > 3) die(15, 3);
        long tid = parse_tid(f[0]);
        long long a = parse_amt(f[1]);
        if (tid < 0) die(15, 3);
        if (a < 0) die(12, 3);
        if (k == 3 && !valid_id(f[2], 24)) die(16, 3);
        items[n].tid = tid;
        items[n].amt = a;
        snprintf(items[n].aux, sizeof items[n].aux, "%s", k == 3 ? f[2] : "");
        items[n].keep = 1;
        n++;
    }
    fclose(in);
    if (n == 0) die(15, 3);

    lock_state();
    for (int i = 0; i < n; i++) {
        pay_t p;
        if (!load_pay(items[i].tid, &p)) die(21, 3);
        if (strcmp(p.mid, mid) != 0) die(22, 3);
    }
    if (batch_seen(bid)) {
        printf("accepted %d\n", n);
        return 0;
    }
    for (int i = 0; i < n; i++)
        for (int j = 0; j < i; j++)
            if (items[j].keep && items[j].tid == items[i].tid && items[j].amt == items[i].amt &&
                strcmp(items[j].aux, items[i].aux) == 0) { items[i].keep = 0; break; }
    for (int i = 0; i < n; i++) {
        if (!items[i].keep) continue;
        int first = 1;
        for (int j = 0; j < i; j++) if (items[j].keep && items[j].tid == items[i].tid) { first = 0; break; }
        if (!first) continue;
        long long sum = 0;
        for (int j = i; j < n; j++) if (items[j].keep && items[j].tid == items[i].tid) sum += items[j].amt;
        pay_t p;
        load_pay(items[i].tid, &p);
        if (refunded_total(items[i].tid) + sum > p.amt) die(23, 3);
    }
    long seq = count_lines("ref.dat");
    FILE *fp = open_append("ref.dat");
    for (int i = 0; i < n; i++) {
        if (!items[i].keep) continue;
        fprintf(fp, "%ld|%s|%ld|%lld|%s\n", ++seq, bid, items[i].tid, items[i].amt, items[i].aux);
    }
    if (fclose(fp) != 0) die(21, 4);
    FILE *bf = open_append("bat.dat");
    fprintf(bf, "%s|%d\n", bid, n);
    if (fclose(bf) != 0) die(21, 4);
    printf("accepted %d\n", n);
    return 0;
}

static int cmd_status(int argc, char **argv) {
    const char *bid = opt(argc, argv, "--batch");
    if (!bid) die(11, 2);
    if (!valid_id(bid, 32)) die(18, 3);
    if (!batch_seen(bid)) die(31, 3);
    char p[1024];
    path(p, sizeof p, "ref.dat");
    FILE *fp = fopen(p, "r");
    if (!fp) return 0;
    ref_t r;
    while (each_ref(&r, fp)) {
        if (strcmp(r.batch, bid) != 0) continue;
        printf("RF%08ld\tT%08ld\t%lld\t%s\n", r.seq, r.tid, r.amt, r.aux[0] ? r.aux : "-");
    }
    fclose(fp);
    return 0;
}

static int cmd_txn(int argc, char **argv) {
    if (argc < 1) die(11, 2);
    long tid = parse_tid(argv[0]);
    if (tid < 0) die(15, 3);
    pay_t p;
    if (!load_pay(tid, &p)) die(21, 3);
    long long refunded = refunded_total(tid);
    printf("tid T%08ld\nmid %s\noid %s\namt %lld\nrefunded %lld\nbal %lld\n",
           tid, p.mid, p.oid, p.amt, refunded, p.amt - refunded);
    return 0;
}

int main(int argc, char **argv) {
    if (argc < 2 || strcmp(argv[1], "--help") == 0 || strcmp(argv[1], "-h") == 0) {
        fputs(USAGE, argc < 2 ? stderr : stdout);
        return argc < 2 ? 2 : 0;
    }
    const char *h = getenv("PGSIM_HOME");
    if (!h || !*h) die(10, 2);
    if (snprintf(home, sizeof home, "%s", h) >= (int)sizeof home) die(10, 2);
    const char *cmd = argv[1];
    if (strcmp(cmd, "pay") == 0) return cmd_pay(argc - 2, argv + 2);
    if (strcmp(cmd, "refund") == 0) return cmd_refund(argc - 2, argv + 2);
    if (strcmp(cmd, "status") == 0) return cmd_status(argc - 2, argv + 2);
    if (strcmp(cmd, "txn") == 0) return cmd_txn(argc - 2, argv + 2);
    die(11, 2);
    return 2;
}
