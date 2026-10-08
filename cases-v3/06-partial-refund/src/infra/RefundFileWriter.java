package infra;

import domain.RefundLine;
import util.Refs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes the PG refund batch file: one line per refund, TID TAB AMOUNT TAB merchant reference. */
public final class RefundFileWriter {
    private RefundFileWriter() {}

    /** Merchant reference printed on the PG settlement file so finance can trace the refund to our order. */
    public static String merchantRef(RefundLine line) {
        return Refs.sanitize(line.orderNo());
    }

    public static String format(List<RefundLine> lines) {
        StringBuilder sb = new StringBuilder();
        for (RefundLine l : lines) {
            sb.append(l.tid()).append('\t').append(l.amount()).append('\t').append(merchantRef(l)).append('\n');
        }
        return sb.toString();
    }

    public static Path write(Path dir, String batchId, List<RefundLine> lines) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(batchId + ".tsv");
            Files.writeString(file, format(lines), StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("cannot write refund batch " + batchId, e);
        }
    }
}
