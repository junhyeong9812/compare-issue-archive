package infra;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Thin wrapper around the PG vendor CLI (vendor/bin/pgsim). One process per call. */
public class PgsimClient {
    /** What the PG reports for one payment. */
    public record Txn(String tid, long amount, long refunded, long balance) {}

    /** One row of a PG batch status: refund number, TID, amount, merchant field. */
    public record RefundRow(String refundNo, String tid, long amount, String aux) {}

    private final Path bin;
    private final Path home;
    private final long timeoutSeconds;

    public PgsimClient(Path bin, Path home, long timeoutSeconds) {
        this.bin = bin;
        this.home = home;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Charges the customer; returns the PG transaction id. */
    public String pay(String mid, long amount, String orderNo) {
        return run(List.of("pay", "--mid", mid, "--amt", Long.toString(amount), "--oid", orderNo)).strip();
    }

    /** Submits one refund batch file; returns the accepted count reported by the PG. */
    public int refund(String mid, String batchId, Path file) {
        String out = run(List.of("refund", "--mid", mid, "--batch", batchId, file.toString())).strip();
        String[] t = out.split("\\s+");
        if (t.length != 2 || !t[0].equals("accepted")) {
            throw new PgException("unexpected refund reply: " + out);
        }
        return Integer.parseInt(t[1]);
    }

    public List<RefundRow> status(String batchId) {
        List<RefundRow> rows = new ArrayList<>();
        for (String line : run(List.of("status", "--batch", batchId)).split("\n")) {
            if (line.isBlank()) continue;
            String[] f = line.split("\t");
            if (f.length != 4) throw new PgException("unexpected status line: " + line);
            rows.add(new RefundRow(f[0], f[1], Long.parseLong(f[2]), f[3].equals("-") ? "" : f[3]));
        }
        return rows;
    }

    public Txn txn(String tid) {
        Map<String, String> kv = new HashMap<>();
        for (String line : run(List.of("txn", tid)).split("\n")) {
            int sp = line.indexOf(' ');
            if (sp > 0) kv.put(line.substring(0, sp), line.substring(sp + 1).strip());
        }
        return new Txn(kv.get("tid"), Long.parseLong(kv.get("amt")), Long.parseLong(kv.get("refunded")),
                Long.parseLong(kv.get("bal")));
    }

    private String run(List<String> args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(bin.toString());
        cmd.addAll(args);
        try {
            Files.createDirectories(home);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("PGSIM_HOME", home.toAbsolutePath().toString());
            Process p = pb.start();
            byte[] out;
            byte[] err;
            try (InputStream o = p.getInputStream(); InputStream e = p.getErrorStream()) {
                out = o.readAllBytes();
                err = e.readAllBytes();
            }
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new PgException("timeout: " + String.join(" ", cmd));
            }
            if (p.exitValue() != 0) {
                throw new PgException("pgsim failed [" + new String(err, StandardCharsets.UTF_8).strip() + "]: "
                        + String.join(" ", cmd));
            }
            return new String(out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PgException("exec failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PgException("interrupted");
        }
    }

    public static class PgException extends RuntimeException {
        public PgException(String msg) {
            super(msg);
        }
    }
}
