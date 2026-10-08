/* fxq — vendor FX quote CLI (blackbox for case 01). Deterministic, no clock/random. */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static const char *PAIRS[] = {"USDKRW", "EURKRW", "JPYKRW", "CNYKRW"};
static const long BASE[] = {138000, 161000, 92500, 19100}; /* cents */
static const long SPAN[] = {1500, 1500, 800, 150};
#define NPAIRS 4

static const int CAL[] = {20260101, 20260216, 20260217, 20260218, 20260302, 20260505,
                          20260525, 20260603, 20260817, 20260924, 20260925, 20260926,
                          20261005, 20261009, 20261225, 20261231};
#define NCAL ((int)(sizeof(CAL) / sizeof(CAL[0])))

static long days_from_civil(long y, unsigned m, unsigned d) {
    y -= m <= 2;
    long era = (y >= 0 ? y : y - 399) / 400;
    unsigned yoe = (unsigned)(y - era * 400);
    unsigned doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
    unsigned doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
    return era * 146097 + (long)doe - 719468;
}

static void civil_from_days(long z, int *y, int *m, int *d) {
    z += 719468;
    long era = (z >= 0 ? z : z - 146096) / 146097;
    unsigned doe = (unsigned)(z - era * 146097);
    unsigned yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
    long yy = (long)yoe + era * 400;
    unsigned doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    unsigned mp = (5 * doy + 2) / 153;
    unsigned dd = doy - (153 * mp + 2) / 5 + 1;
    unsigned mm = mp < 10 ? mp + 3 : mp - 9;
    *y = (int)(yy + (mm <= 2));
    *m = (int)mm;
    *d = (int)dd;
}

static int open_day(long z) {
    long wd = (z + 4) % 7; /* 0 = Sunday */
    if (wd < 0) wd += 7;
    if (wd == 0 || wd == 6) return 0;
    int y, m, d;
    civil_from_days(z, &y, &m, &d);
    int key = y * 10000 + m * 100 + d;
    for (int i = 0; i < NCAL; i++)
        if (CAL[i] == key) return 0;
    return 1;
}

static unsigned long mix(int p, long z, unsigned long salt) {
    unsigned long h = 1469598103934665603UL ^ salt;
    const char *s = PAIRS[p];
    for (; *s; s++) { h ^= (unsigned char)*s; h *= 1099511628211UL; }
    for (int i = 0; i < 4; i++) { h ^= (unsigned long)((z >> (8 * i)) & 0xff); h *= 1099511628211UL; }
    h ^= h >> 29; h *= 0xbf58476d1ce4e5b9UL; h ^= h >> 32;
    return h;
}

static long base_val(int p, long z) {
    long span = SPAN[p];
    return BASE[p] + (long)(mix(p, z, 0) % (unsigned long)(2 * span + 1)) - span;
}

static int parse_date(const char *s, long *z) {
    if (strlen(s) != 10 || s[4] != '-' || s[7] != '-') return 0;
    for (int i = 0; i < 10; i++)
        if (i != 4 && i != 7 && (s[i] < '0' || s[i] > '9')) return 0;
    int y = atoi(s), m = atoi(s + 5), d = atoi(s + 8);
    if (m < 1 || m > 12 || d < 1 || d > 31) return 0;
    long zz = days_from_civil(y, (unsigned)m, (unsigned)d);
    int yy, mm, dd;
    civil_from_days(zz, &yy, &mm, &dd);
    if (yy != y || mm != m || dd != d) return 0;
    *z = zz;
    return 1;
}

static int fail(int code) {
    fprintf(stderr, "E%d\n", code);
    return 2;
}

static void usage(FILE *f) {
    fputs("usage: fxq rate <PAIR> <YYYY-MM-DD> [--fix]\n"
          "       fxq pairs\n"
          "       fxq --version\n", f);
}

int main(int argc, char **argv) {
    if (argc < 2) { usage(stderr); return fail(11); }
    if (!strcmp(argv[1], "--help") || !strcmp(argv[1], "-h")) { usage(stdout); return 0; }
    if (!strcmp(argv[1], "--version")) { puts("fxq 3.4.1"); return 0; }
    if (!strcmp(argv[1], "pairs")) {
        for (int i = 0; i < NPAIRS; i++) puts(PAIRS[i]);
        return 0;
    }
    if (strcmp(argv[1], "rate")) return fail(11);

    const char *pair = NULL, *date = NULL;
    int fix = 0;
    for (int i = 2; i < argc; i++) {
        if (!strcmp(argv[i], "--fix")) fix = 1;
        else if (!pair) pair = argv[i];
        else if (!date) date = argv[i];
        else return fail(11);
    }
    if (!pair || !date) return fail(11);
    int p = -1;
    for (int i = 0; i < NPAIRS; i++)
        if (!strcmp(pair, PAIRS[i])) p = i;
    if (p < 0) return fail(14);
    long z;
    if (!parse_date(date, &z)) return fail(12);
    if (z < days_from_civil(2020, 1, 1) || z > days_from_civil(2030, 12, 31)) return fail(13);

    long v;
    int lag = 0;
    if (open_day(z)) {
        v = base_val(p, z);
    } else {
        long a = z - 1, b = z + 1;
        while (!open_day(a)) a--;
        while (!open_day(b)) b++;
        if (fix) {
            long adj = (long)(mix(p, z, 0x5f3759dfUL) % 41) - 20;
            v = (base_val(p, a) + base_val(p, b)) / 2 + adj;
        } else {
            v = base_val(p, a);
            lag = (int)(z - a);
        }
    }
    if (lag) printf("%s %s %ld.%02ld ~%d\n", pair, date, v / 100, v % 100, lag);
    else printf("%s %s %ld.%02ld\n", pair, date, v / 100, v % 100);
    return 0;
}
