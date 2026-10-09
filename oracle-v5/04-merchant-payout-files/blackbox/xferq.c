/*
 * xferq — mock of the partner file-transfer vendor CLI (Hanbit Pay, experiment v3).
 *
 * Undocumented rule (the case's black box):
 *   `send` accepts any well-formed name and always prints a ticket with exit 0,
 *   but when the remote file name contains four or more '_' characters the
 *   transfer is stored with state 6 (parked) instead of 2 (in the partner box).
 *   `ls` and `pull` only see state-2 transfers, so parked files never reach the
 *   partner. `status` prints the state as an opaque number.
 *
 * Errors are opaque codes on stderr only (E11..E31). Deterministic: no clock,
 * no randomness; state lives in $XFERQ_HOME (spool.idx + obj/).
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/file.h>
#include <sys/stat.h>

#define MAXLINE 1024
#define MAXFILE (4 * 1024 * 1024)

static const char *USAGE =
    "usage: xferq <command> [options]\n"
    "\n"
    "commands:\n"
    "  send    --to PARTNER --file PATH [--as NAME]\n"
    "          prints the ticket\n"
    "  status  TICKET\n"
    "          ticket, partner, name, bytes, st (tab separated)\n"
    "  ls      --to PARTNER\n"
    "          name, bytes, ticket (tab separated)\n"
    "  pull    --to PARTNER --name NAME\n"
    "          writes the file to stdout\n"
    "\n"
    "environment:\n"
    "  XFERQ_HOME   spool directory (required)\n";

static void die(int code, int rc) {
    fprintf(stderr, "E%02d\n", code);
    exit(rc);
}

static const char *home_dir(void) {
    const char *h = getenv("XFERQ_HOME");
    if (!h || !*h) die(10, 2);
    if (strlen(h) > 800) die(10, 2);
    return h;
}

static void path_of(char *buf, size_t n, const char *leaf) {
    if (snprintf(buf, n, "%s/%s", home_dir(), leaf) >= (int)n) die(10, 2);
}

static void obj_path(char *buf, size_t n, long seq) {
    if (snprintf(buf, n, "%s/obj/%08ld", home_dir(), seq) >= (int)n) die(10, 2);
}

static int valid_partner(const char *s) {
    size_t n = s ? strlen(s) : 0;
    if (n < 2 || n > 10) return 0;
    for (size_t i = 0; i < n; i++)
        if (!((s[i] >= 'A' && s[i] <= 'Z') || (s[i] >= '0' && s[i] <= '9'))) return 0;
    return 1;
}

static int valid_name(const char *s) {
    size_t n = s ? strlen(s) : 0;
    if (n == 0 || n > 120 || s[0] == '.') return 0;
    for (size_t i = 0; i < n; i++) {
        unsigned char c = (unsigned char)s[i];
        int ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
                 c == '.' || c == '-' || c == 0x5f;
        if (!ok) return 0;
    }
    return 1;
}

/* routing class of a remote name */
static int classify(const char *name) {
    int k = 0;
    for (const char *p = name; *p; p++) k += (*p == 0x5f);
    return k >= (1 << 2) ? 6 : 2;
}

typedef struct {
    long seq;
    int st;
    char partner[16];
    char name[160];
    long bytes;
} rec_t;

static int parse_rec(char *line, rec_t *r) {
    char *f[5];
    int n = 0;
    char *p = line;
    size_t len = strlen(line);
    if (len && line[len - 1] == '\n') line[len - 1] = 0;
    while (n < 5) {
        f[n++] = p;
        char *bar = strchr(p, '|');
        if (!bar) break;
        *bar = 0;
        p = bar + 1;
    }
    if (n != 5) return 0;
    r->seq = atol(f[0]);
    r->st = atoi(f[1]);
    snprintf(r->partner, sizeof r->partner, "%s", f[2]);
    snprintf(r->name, sizeof r->name, "%s", f[3]);
    r->bytes = atol(f[4]);
    return 1;
}

static const char *opt(int argc, char **argv, const char *name) {
    for (int i = 0; i + 1 < argc; i++)
        if (strcmp(argv[i], name) == 0) return argv[i + 1];
    return NULL;
}

static const char *base_name(const char *path) {
    const char *s = strrchr(path, '/');
    return s ? s + 1 : path;
}

static int cmd_send(int argc, char **argv) {
    const char *to = opt(argc, argv, "--to");
    const char *file = opt(argc, argv, "--file");
    const char *as = opt(argc, argv, "--as");
    if (!to || !file) die(11, 2);
    if (!valid_partner(to)) die(14, 3);
    const char *name = as ? as : base_name(file);
    if (!valid_name(name)) die(16, 3);

    struct stat sb;
    if (stat(file, &sb) != 0 || !S_ISREG(sb.st_mode) || sb.st_size > MAXFILE) die(15, 3);
    FILE *in = fopen(file, "rb");
    if (!in) die(15, 3);
    static char data[MAXFILE];
    size_t got = fread(data, 1, sizeof data, in);
    fclose(in);
    if (got != (size_t)sb.st_size) die(15, 3);

    char idx[1024], dir[1024], obj[1024];
    path_of(dir, sizeof dir, "obj");
    mkdir(dir, 0755);
    path_of(idx, sizeof idx, "spool.idx");
    int fd = open(idx, O_RDWR | O_CREAT, 0644);
    if (fd < 0) die(21, 4);
    if (flock(fd, LOCK_EX) != 0) die(21, 4);
    FILE *fp = fdopen(fd, "r+");
    if (!fp) die(21, 4);
    char line[MAXLINE];
    long seq = 0;
    while (fgets(line, sizeof line, fp)) seq++;
    seq++;

    obj_path(obj, sizeof obj, seq);
    FILE *out = fopen(obj, "wb");
    if (!out) die(21, 4);
    if (fwrite(data, 1, got, out) != got || fclose(out) != 0) die(21, 4);

    fseek(fp, 0, SEEK_END);
    fprintf(fp, "%ld|%d|%s|%s|%ld\n", seq, classify(name), to, name, (long)got);
    if (fflush(fp) != 0) die(21, 4);
    fclose(fp);
    printf("T%08ld\n", seq);
    return 0;
}

static FILE *open_index(void) {
    char idx[1024];
    path_of(idx, sizeof idx, "spool.idx");
    return fopen(idx, "r");
}

/* latest state-2 record per name for a partner; returns count */
static int box(const char *to, rec_t *out, int cap) {
    FILE *fp = open_index();
    if (!fp) return 0;
    int n = 0;
    char line[MAXLINE];
    rec_t r;
    while (fgets(line, sizeof line, fp)) {
        if (!parse_rec(line, &r) || strcmp(r.partner, to) != 0 || r.st != 2) continue;
        int i;
        for (i = 0; i < n; i++)
            if (strcmp(out[i].name, r.name) == 0) break;
        if (i < n) out[i] = r;
        else if (n < cap) out[n++] = r;
    }
    fclose(fp);
    return n;
}

static int cmd_ls(int argc, char **argv) {
    const char *to = opt(argc, argv, "--to");
    if (!to) die(11, 2);
    if (!valid_partner(to)) die(14, 3);
    static rec_t recs[4096];
    int n = box(to, recs, 4096);
    for (int i = 0; i < n; i++)
        printf("%s\t%ld\tT%08ld\n", recs[i].name, recs[i].bytes, recs[i].seq);
    return 0;
}

static int cmd_pull(int argc, char **argv) {
    const char *to = opt(argc, argv, "--to");
    const char *name = opt(argc, argv, "--name");
    if (!to || !name) die(11, 2);
    if (!valid_partner(to)) die(14, 3);
    static rec_t recs[4096];
    int n = box(to, recs, 4096);
    for (int i = 0; i < n; i++) {
        if (strcmp(recs[i].name, name) != 0) continue;
        char obj[1024];
        obj_path(obj, sizeof obj, recs[i].seq);
        FILE *in = fopen(obj, "rb");
        if (!in) die(21, 4);
        char buf[8192];
        size_t k;
        while ((k = fread(buf, 1, sizeof buf, in)) > 0) fwrite(buf, 1, k, stdout);
        fclose(in);
        return 0;
    }
    die(31, 3);
    return 3;
}

static int cmd_status(int argc, char **argv) {
    if (argc < 1) die(11, 2);
    const char *t = argv[0];
    if (t[0] != 'T' || strlen(t) != 9) die(31, 3);
    long want = atol(t + 1);
    FILE *fp = open_index();
    if (!fp) die(31, 3);
    char line[MAXLINE];
    rec_t r;
    while (fgets(line, sizeof line, fp)) {
        if (!parse_rec(line, &r) || r.seq != want) continue;
        printf("T%08ld\t%s\t%s\t%ld\t%d\n", r.seq, r.partner, r.name, r.bytes, r.st);
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
    const char *cmd = argv[1];
    if (strcmp(cmd, "--version") == 0) { puts("xferq 3.1.0"); return 0; }
    if (strcmp(cmd, "send") == 0) return cmd_send(argc - 2, argv + 2);
    if (strcmp(cmd, "status") == 0) return cmd_status(argc - 2, argv + 2);
    if (strcmp(cmd, "ls") == 0) return cmd_ls(argc - 2, argv + 2);
    if (strcmp(cmd, "pull") == 0) return cmd_pull(argc - 2, argv + 2);
    die(11, 2);
    return 2;
}
