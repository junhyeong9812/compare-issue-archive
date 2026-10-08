import config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Tiny assertion runner (no external test framework on the batch host). */
final class TestSupport {
    private final String suite;
    private final List<String> failures = new ArrayList<>();
    private int passed;

    TestSupport(String suite) {
        this.suite = suite;
    }

    interface Body { void run() throws Exception; }

    void test(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("ok   - " + name);
        } catch (AssertionError | Exception e) {
            failures.add(name + ": " + e.getMessage());
            System.out.println("FAIL - " + name + ": " + e.getMessage());
        }
    }

    static void eq(Object expected, Object actual, String msg) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(msg + " (expected " + expected + ", got " + actual + ")");
        }
    }

    static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    /** Fresh points-vendor ledger per test. */
    static AppConfig sandbox() throws IOException {
        Path tmp = Files.createTempDirectory("points-");
        tmp.toFile().deleteOnExit();
        return AppConfig.load().with("pts.home", tmp.resolve("pts").toString());
    }

    void finish() {
        System.out.printf("%s: %d passed, %d failed%n", suite, passed, failures.size());
        if (!failures.isEmpty()) System.exit(1);
    }
}
