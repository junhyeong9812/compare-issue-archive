/*
 * idchk — mock of the identity-verification (KYC) vendor CLI (Hanbit Pay, experiment v3).
 *
 * Undocumented rule (the case's black box):
 *   `batch --in FILE` validates every line (any malformed line rejects the
 *   whole file with an opaque code), then prints "<job>\t<lines read>" and
 *   exits 0 — but only the first 12 lines of each call are actually checked.
 *   Lines after the 12th are dropped without a trace: no result is stored,
 *   so `result REF` answers E31 for them forever. `job JOBID` shows the line
 *   count and the per-verdict counters (v1 + v2 < in when lines were dropped).
 *
 * Verdict = deterministic hash of name|birth|phone (no clock, no randomness).
 * State lives in $IDCHK_HOME (jobs.dat, results.dat).
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/file.h>

#define MAXLINE 1024
#define MAXLINES 1000

static const char *USAGE =
    "usage: idchk <command> [options]\n"
    "\n"
    "commands:\n"
    "  batch   --in FILE\n"
    "          FILE: ref, name, birth (YYYYMMDD), phone (tab separated)\n"
    "          prints the job id and the number of lines read\n"
    "  result  REF\n"
    "  job     JOBID\n"
    "  --version\n"
    "\n"
    "environment:\n"
    "  IDCHK_HOME   working directory (required)\n";

static void die(int code, int rc) {
    fprintf(stderr, "E%02d\n", code);
    exit(rc);
}

static const char *state_path(const char *name) {
    static char buf[3][1024];
    static int k = 0;
    const char *home = getenv("IDCHK_HOME");
    if (!home || !*home) die(10, 2);
    k = (k + 1) % 3;
    if (snprintf(buf[k], sizeof buf[k], "%s/%s", home, name) >= (int)sizeof buf[k]) die(10, 2);
    return buf[k];
}

static int valid_ref(const char *s) {
    size_t n = strlen(s);
    if (n == 0 || n > 20) return 0;
    for (size_t i = 0; i < n; i++) {
        unsigned char c = (unsigned char)s[i];
        int ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '-';
        if (!ok) return 0;
    }
    return 1;
}

static int valid_text(const unsigned char *s) {
    size_t i = 0;
    if (!s[0] || strlen((const char *)s) > 120) return 0;
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

static int valid_birth(const char *s) {
    if (strlen(s) != 8) return 0;
    for (int i = 0; i < 8; i++) if (s[i] < '0' || s[i] > '9') return 0;
    int y = (s[0]-'0')*1000 + (s[1]-'0')*100 + (s[2]-'0')*10 + (s[3]-'0');
    int m = (s[4]-'0')*10 + (s[5]-'0');
    int d = (s[6]-'0')*10 + (s[7]-'0');
    return y >= 1900 && y <= 2099 && m >= 1 && m <= 12 && d >= 1 && d <= 31;
}

static int valid_phone(const char *s) {
    size_t n = strlen(s);
    if (n < 10 || n > 11 || s[0] != '0' || s[1] != '1') return 0;
    for (size_t i = 0; i < n; i++) if (s[i] < '0' || s[i] > '9') return 0;
    return 1;
}

static unsigned verdict(const char *name, const char *birth, const char *phone) {
    unsigned h = 2166136261u;
    const char *parts[3] = {name, birth, phone};
    for (int p = 0; p < 3; p++) {
        for (const unsigned char *c = (const unsigned char *)parts[p]; *c; c++) { h ^= *c; h *= 16777619u; }
        h ^= '|'; h *= 16777619u;
    }
    return (h % 7u) == 3u ? 2u : 1u;
}

typedef struct { char ref[32]; char name[160]; char birth[16]; char phone[16]; } req_t;

static req_t reqs[MAXLINES];

static void chomp(char *s) {
    size_t n = strlen(s);
    while (n && (s[n - 1] == '\n' || s[n - 1] == '\r')) s[--n] = 0;
}

/* exactly max fields separated by tabs; returns the field count found */
static int split_tab(char *line, char **f, int max) {
    int tabs = 0;
    for (char *c = line; *c; c++) if (*c == '\t') tabs++;
    if (tabs != max - 1) return tabs + 1;
    char *p = line;
    for (int n = 0; n < max; n++) {
        f[n] = p;
        char *t = strchr(p, '\t');
        if (t) { *t = 0; p = t + 1; }
    }
    return max;
}

static long count_lines(const char *path) {
    FILE *fp = fopen(path, "r");
    if (!fp) return 0;
    long n = 0;
    char line[MAXLINE];
    while (fgets(line, sizeof line, fp)) n++;
    fclose(fp);
    return n;
}

static int cmd_batch(int argc, char **argv) {
    const char *in = NULL;
    for (int i = 0; i + 1 < argc; i++) if (strcmp(argv[i], "--in") == 0) in = argv[i + 1];
    if (!in) die(11, 2);
    FILE *fp = fopen(in, "r");
    if (!fp) die(12, 3);
    char line[MAXLINE];
    int n = 0;
    while (fgets(line, sizeof line, fp)) {
        chomp(line);
        if (!line[0]) continue;
        if (n >= MAXLINES) die(19, 3);
        char *f[4];
        if (split_tab(line, f, 4) != 4) die(13, 3);
        if (!valid_ref(f[0])) die(14, 3);
        if (!valid_text((const unsigned char *)f[1])) die(15, 3);
        if (!valid_birth(f[2])) die(16, 3);
        if (!valid_phone(f[3])) die(17, 3);
        snprintf(reqs[n].ref, sizeof reqs[n].ref, "%s", f[0]);
        snprintf(reqs[n].name, sizeof reqs[n].name, "%s", f[1]);
        snprintf(reqs[n].birth, sizeof reqs[n].birth, "%s", f[2]);
        snprintf(reqs[n].phone, sizeof reqs[n].phone, "%s", f[3]);
        n++;
    }
    fclose(fp);
    if (n == 0) die(18, 3);

    int lock = open(state_path("idchk.lock"), O_RDWR | O_CREAT, 0644);
    if (lock < 0 || flock(lock, LOCK_EX) != 0) die(21, 4);
    const char *jobs = state_path("jobs.dat");
    long job = count_lines(jobs) + 1;
    FILE *rf = fopen(state_path("results.dat"), "a");
    if (!rf) die(21, 4);
    int take = n < (3 << 2) ? n : (3 << 2);
    int c1 = 0, c2 = 0;
    for (int i = 0; i < take; i++) {
        unsigned v = verdict(reqs[i].name, reqs[i].birth, reqs[i].phone);
        if (v == 1) c1++; else c2++;
        fprintf(rf, "%s|%ld|%u\n", reqs[i].ref, job, v);
    }
    if (fclose(rf) != 0) die(21, 4);
    FILE *jf = fopen(jobs, "a");
    if (!jf) die(21, 4);
    fprintf(jf, "%ld|%d|%d|%d\n", job, n, c1, c2);
    if (fclose(jf) != 0) die(21, 4);
    close(lock);
    printf("J%07ld\t%d\n", job, n);
    return 0;
}

static int cmd_result(int argc, char **argv) {
    if (argc < 1) die(11, 2);
    if (!valid_ref(argv[0])) die(14, 3);
    FILE *fp = fopen(state_path("results.dat"), "r");
    if (!fp) die(31, 3);
    char line[MAXLINE];
    long job = 0;
    unsigned v = 0;
    while (fgets(line, sizeof line, fp)) {
        chomp(line);
        char *bar1 = strchr(line, '|');
        if (!bar1) continue;
        *bar1 = 0;
        if (strcmp(line, argv[0]) != 0) continue;
        char *bar2 = strchr(bar1 + 1, '|');
        if (!bar2) continue;
        *bar2 = 0;
        job = atol(bar1 + 1);
        v = (unsigned)atoi(bar2 + 1);
    }
    fclose(fp);
    if (!job) die(31, 3);
    printf("ref %s\njob J%07ld\nv %u\n", argv[0], job, v);
    return 0;
}

static int cmd_job(int argc, char **argv) {
    if (argc < 1) die(11, 2);
    const char *id = argv[0];
    if (id[0] != 'J' || strlen(id) != 8) die(32, 3);
    long want = atol(id + 1);
    FILE *fp = fopen(state_path("jobs.dat"), "r");
    if (!fp) die(32, 3);
    char line[MAXLINE];
    long job; int in, c1, c2;
    while (fgets(line, sizeof line, fp)) {
        if (sscanf(line, "%ld|%d|%d|%d", &job, &in, &c1, &c2) != 4 || job != want) continue;
        printf("job J%07ld\nin %d\nv1 %d\nv2 %d\n", job, in, c1, c2);
        fclose(fp);
        return 0;
    }
    fclose(fp);
    die(32, 3);
    return 3;
}

int main(int argc, char **argv) {
    if (argc < 2 || strcmp(argv[1], "--help") == 0 || strcmp(argv[1], "-h") == 0) {
        fputs(USAGE, argc < 2 ? stderr : stdout);
        return argc < 2 ? 2 : 0;
    }
    if (strcmp(argv[1], "--version") == 0) {
        puts("idchk 3.2.0");
        return 0;
    }
    const char *cmd = argv[1];
    if (strcmp(cmd, "batch") == 0) return cmd_batch(argc - 2, argv + 2);
    if (strcmp(cmd, "result") == 0) return cmd_result(argc - 2, argv + 2);
    if (strcmp(cmd, "job") == 0) return cmd_job(argc - 2, argv + 2);
    die(11, 2);
    return 2;
}
