package infra;

import domain.FxQuote;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Thin wrapper around the vendor quote CLI. One process per call. */
public class FxqClient {
    private final Path bin;
    private final long timeoutSeconds;

    public FxqClient(Path bin, long timeoutSeconds) {
        this.bin = bin;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Raw stdout line for {@code fxq rate PAIR DATE}. */
    public String rate(String pair, LocalDate date) {
        return rate(pair, date, false);
    }

    /** Raw stdout line for {@code fxq rate PAIR DATE --src 3} (settlement source). */
    public String settlementQuote(String pair, LocalDate date) {
        return rate(pair, date, true);
    }

    private String rate(String pair, LocalDate date, boolean settlementSource) {
        List<String> cmd = new ArrayList<>();
        cmd.add(bin.toString());
        cmd.add("rate");
        cmd.add(pair);
        cmd.add(date.toString());
        if (settlementSource) {
            cmd.add("--src");
            cmd.add("3");
        }
        return run(cmd);
    }

    /** Parses one quote line: {@code PAIR YYYY-MM-DD RATE [~N]}. */
    public static FxQuote parse(String line) {
        String[] t = line.trim().split("\\s+");
        if (t.length < 3) {
            throw new IllegalArgumentException("unexpected quote line: " + line);
        }
        boolean lagged = t.length > 3 && t[3].startsWith("~");
        return new FxQuote(t[0], LocalDate.parse(t[1]), new BigDecimal(t[2]), lagged);
    }

    private String run(List<String> cmd) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            byte[] out;
            byte[] err;
            try (InputStream o = p.getInputStream(); InputStream e = p.getErrorStream()) {
                out = o.readAllBytes();
                err = e.readAllBytes();
            }
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new FxqException("timeout", cmd);
            }
            if (p.exitValue() != 0) {
                throw new FxqException(new String(err, StandardCharsets.UTF_8).trim(), cmd);
            }
            String line = new String(out, StandardCharsets.UTF_8).trim();
            if (line.isEmpty()) throw new FxqException("empty output", cmd);
            return line;
        } catch (IOException e) {
            throw new FxqException("exec failed: " + e.getMessage(), cmd);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FxqException("interrupted", cmd);
        }
    }

    public static class FxqException extends RuntimeException {
        public FxqException(String code, List<String> cmd) {
            super("fxq failed [" + code + "]: " + String.join(" ", cmd));
        }
    }
}
