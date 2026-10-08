package infra;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Thin wrapper around the points vendor CLI (vendor/bin/ptsctl). One process per call. */
public class PtsctlClient {
    /** One line of the vendor history: entry number, our reference, points. */
    public record HistoryRow(String entryNo, String ref, long points) {}

    private final Path bin;
    private final Path home;
    private final long timeoutSeconds;

    public PtsctlClient(Path bin, Path home, long timeoutSeconds) {
        this.bin = bin;
        this.home = home;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Credits points to a member; returns the vendor entry number. */
    public String earn(String memberNo, long points, String ref) {
        return run(List.of("earn", "--mbr", memberNo, "--pt", Long.toString(points), "--ref", ref)).strip();
    }

    public long balance(String memberNo) {
        String out = run(List.of("bal", memberNo)).strip();
        String[] t = out.split("\\s+");
        if (t.length != 2) throw new PtsException("unexpected bal reply: " + out);
        return Long.parseLong(t[1]);
    }

    public List<HistoryRow> history(String memberNo) {
        List<HistoryRow> rows = new ArrayList<>();
        for (String line : run(List.of("hist", memberNo)).split("\n")) {
            if (line.isBlank()) continue;
            String[] f = line.split("\t");
            if (f.length != 3) throw new PtsException("unexpected hist line: " + line);
            rows.add(new HistoryRow(f[0], f[1], Long.parseLong(f[2])));
        }
        return rows;
    }

    private String run(List<String> args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(bin.toString());
        cmd.addAll(args);
        try {
            Files.createDirectories(home);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("PTSCTL_HOME", home.toAbsolutePath().toString());
            Process p = pb.start();
            byte[] out;
            byte[] err;
            try (InputStream o = p.getInputStream(); InputStream e = p.getErrorStream()) {
                out = o.readAllBytes();
                err = e.readAllBytes();
            }
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new PtsException("timeout: " + String.join(" ", cmd));
            }
            if (p.exitValue() != 0) {
                throw new PtsException("ptsctl failed [" + new String(err, StandardCharsets.UTF_8).strip() + "]: "
                        + String.join(" ", cmd));
            }
            return new String(out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PtsException("exec failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PtsException("interrupted");
        }
    }

    public static class PtsException extends RuntimeException {
        public PtsException(String msg) {
            super(msg);
        }
    }
}
