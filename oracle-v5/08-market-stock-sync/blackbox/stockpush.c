/*
 * stockpush — mock of the open-market stock sync vendor CLI (Hanbit Pay, experiment v3).
 *
 * Undocumented rule (the case's black box):
 *   `push --in FILE` takes "SKU<TAB>QTY" lines. A QTY that parses to zero
 *   ("0", "00", ...) is treated as "no change": the line is counted in the
 *   printed total and exit is 0, but the item keeps its previous quantity and
 *   state (and an unknown SKU is not created). To take an item to zero the
 *   line must carry the vendor's own token "-": quantity 0, state 2.
 *   Any positive quantity sets the quantity and state 1.
 *   `list` / `show` print the state as an opaque number and the transfer that
 *   last touched the item.
 *
 * Errors are opaque codes on stderr only. Deterministic: no clock, no randomness;
 * state lives in $STOCKPUSH_HOME (items.dat, tx.dat).
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/file.h>

#define MAXLINE 512
#define MAXITEMS 4096

static const char *USAGE =
    "usage: stockpush <command> [options]\n"
    "\n"
    "commands:\n"
    "  push  --in FILE\n"
    "        FILE: one item per line, SKU<TAB>QTY   (QTY: digits or \"-\")\n"
    "        prints the transfer id and the number of lines\n"
    "  list\n"
    "        sku, qty, st, transfer (tab separated)\n"
    "  show  SKU\n"
    "  --version\n"
    "\n"
    "environment:\n"
    "  STOCKPUSH_HOME   store directory (required)\n";

static void die(int code, int rc) {
    fprintf(stderr, "E%02d\n", code);
    exit(rc);
}

static const char *state_path(const char *name) {
    static char buf[3][1024];
    static int k = 0;
    const char *home = getenv("STOCKPUSH_HOME");
    if (!home || !*home) die(10, 2);
    k = (k + 1) % 3;
    if (snprintf(buf[k], sizeof buf[k], "%s/%s", home, name) >= (int)sizeof buf[k]) die(10, 2);
    return buf[k];
}

static int valid_sku(const char *s) {
    size_t n = strlen(s);
    if (n < 3 || n > 24) return 0;
    for (size_t i = 0; i < n; i++) {
        unsigned char c = (unsigned char)s[i];
        int ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '-';
        if (!ok) return 0;
    }
    return 1;
}

typedef struct { char sku[32]; long qty; int st; long tx; } item_t;

static item_t items[MAXITEMS];
static int nitems = 0;

static void chomp(char *s) {
    size_t n = strlen(s);
    while (n && (s[n - 1] == '\n' || s[n - 1] == '\r')) s[--n] = 0;
}

static void load(void) {
    FILE *fp = fopen(state_path("items.dat"), "r");
    if (!fp) return;
    char line[MAXLINE];
    while (fgets(line, sizeof line, fp) && nitems < MAXITEMS) {
        item_t *it = &items[nitems];
        if (sscanf(line, "%31[^|]|%ld|%d|%ld", it->sku, &it->qty, &it->st, &it->tx) == 4) nitems++;
    }
    fclose(fp);
}

static void save(void) {
    char tmp[1100];
    snprintf(tmp, sizeof tmp, "%s.tmp", state_path("items.dat"));
    FILE *fp = fopen(tmp, "w");
    if (!fp) die(21, 4);
    for (int i = 0; i < nitems; i++)
        fprintf(fp, "%s|%ld|%d|%ld\n", items[i].sku, items[i].qty, items[i].st, items[i].tx);
    if (fclose(fp) != 0) die(21, 4);
    if (rename(tmp, state_path("items.dat")) != 0) die(21, 4);
}

static item_t *find(const char *sku) {
    for (int i = 0; i < nitems; i++) if (strcmp(items[i].sku, sku) == 0) return &items[i];
    return NULL;
}

static item_t *find_or_add(const char *sku) {
    item_t *it = find(sku);
    if (it) return it;
    if (nitems >= MAXITEMS) die(19, 3);
    it = &items[nitems++];
    snprintf(it->sku, sizeof it->sku, "%s", sku);
    it->qty = 0; it->st = 0; it->tx = 0;
    return it;
}

typedef struct { char sku[32]; long qty; int dash; } line_t;
static line_t lines[MAXITEMS];

static int cmd_push(int argc, char **argv) {
    const char *in = NULL;
    for (int i = 0; i + 1 < argc; i++) if (strcmp(argv[i], "--in") == 0) in = argv[i + 1];
    if (!in) die(11, 2);
    FILE *fp = fopen(in, "r");
    if (!fp) die(12, 3);
    char buf[MAXLINE];
    int n = 0;
    while (fgets(buf, sizeof buf, fp)) {
        chomp(buf);
        if (!buf[0]) continue;
        if (n >= MAXITEMS) die(19, 3);
        char *tab = strchr(buf, '\t');
        if (!tab || strchr(tab + 1, '\t')) die(13, 3);
        *tab = 0;
        const char *sku = buf, *q = tab + 1;
        if (!valid_sku(sku)) die(14, 3);
        line_t *ln = &lines[n];
        snprintf(ln->sku, sizeof ln->sku, "%s", sku);
        ln->dash = 0;
        if (q[0] == '-' && q[1] == 0) {
            ln->dash = 1; ln->qty = 0;
        } else {
            size_t len = strlen(q);
            if (len == 0 || len > 5) die(15, 3);
            for (size_t i = 0; i < len; i++) if (q[i] < '0' || q[i] > '9') die(15, 3);
            ln->qty = atol(q);
        }
        n++;
    }
    fclose(fp);
    if (n == 0) die(18, 3);

    int lock = open(state_path("stockpush.lock"), O_RDWR | O_CREAT, 0644);
    if (lock < 0 || flock(lock, LOCK_EX) != 0) die(21, 4);
    load();
    long tx = 1;
    FILE *tf = fopen(state_path("tx.dat"), "r");
    if (tf) { char t[64]; while (fgets(t, sizeof t, tf)) tx++; fclose(tf); }
    for (int i = 0; i < n; i++) {
        line_t *ln = &lines[i];
        if (ln->dash) {
            item_t *it = find_or_add(ln->sku);
            it->qty = 0; it->st = 2; it->tx = tx;
        } else if (ln->qty) {
            item_t *it = find_or_add(ln->sku);
            it->qty = ln->qty; it->st = 1; it->tx = tx;
        }
    }
    save();
    tf = fopen(state_path("tx.dat"), "a");
    if (!tf) die(21, 4);
    fprintf(tf, "%ld|%d\n", tx, n);
    if (fclose(tf) != 0) die(21, 4);
    close(lock);
    printf("T%07ld\t%d\n", tx, n);
    return 0;
}

static int cmd_list(void) {
    load();
    for (int i = 0; i < nitems; i++)
        printf("%s\t%ld\t%d\tT%07ld\n", items[i].sku, items[i].qty, items[i].st, items[i].tx);
    return 0;
}

static int cmd_show(int argc, char **argv) {
    if (argc < 1) die(11, 2);
    if (!valid_sku(argv[0])) die(14, 3);
    load();
    item_t *it = find(argv[0]);
    if (!it) die(31, 3);
    printf("sku %s\nqty %ld\nst %d\ntransfer T%07ld\n", it->sku, it->qty, it->st, it->tx);
    return 0;
}

int main(int argc, char **argv) {
    if (argc < 2 || strcmp(argv[1], "--help") == 0 || strcmp(argv[1], "-h") == 0) {
        fputs(USAGE, argc < 2 ? stderr : stdout);
        return argc < 2 ? 2 : 0;
    }
    const char *cmd = argv[1];
    if (strcmp(cmd, "--version") == 0) { puts("stockpush 2.3.0"); return 0; }
    if (strcmp(cmd, "push") == 0) return cmd_push(argc - 2, argv + 2);
    if (strcmp(cmd, "list") == 0) return cmd_list();
    if (strcmp(cmd, "show") == 0) return cmd_show(argc - 2, argv + 2);
    die(11, 2);
    return 2;
}
