package infra;

import domain.Partner;
import domain.StatementRow;
import domain.Transaction;
import util.Csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CSV files in the data dir:
 *   transactions.csv       acquirer feed (approved_at in UTC)
 *   partners.csv           partner master
 *   partner-statement.csv  statements sent by partners (gross KRW per KST date)
 */
public class DataFiles {
    private final Path dir;
    private Map<String, Partner> partners;

    public DataFiles(Path dir) {
        this.dir = dir;
    }

    public List<Transaction> transactions() {
        List<Transaction> out = new ArrayList<>();
        for (List<String> c : rows("transactions.csv")) {
            out.add(new Transaction(c.get(0), c.get(1), Instant.parse(c.get(2)), c.get(3),
                    new BigDecimal(c.get(4)), Transaction.Type.valueOf(c.get(5))));
        }
        return out;
    }

    public Optional<Partner> partner(String id) {
        if (partners == null) {
            partners = new LinkedHashMap<>();
            for (List<String> c : rows("partners.csv")) {
                partners.put(c.get(0), new Partner(c.get(0), c.get(1), Integer.parseInt(c.get(2)), "Y".equals(c.get(3))));
            }
        }
        return Optional.ofNullable(partners.get(id));
    }

    public List<StatementRow> statements(LocalDate from, LocalDate to) {
        List<StatementRow> out = new ArrayList<>();
        for (List<String> c : rows("partner-statement.csv")) {
            LocalDate d = LocalDate.parse(c.get(0));
            if (d.isBefore(from) || d.isAfter(to)) continue;
            out.add(new StatementRow(d, c.get(1), Integer.parseInt(c.get(2)), Long.parseLong(c.get(3))));
        }
        return out;
    }

    /** Data rows (header and comment lines skipped), cells trimmed. */
    private List<List<String>> rows(String name) {
        List<String> lines;
        try {
            lines = Files.readAllLines(dir.resolve(name), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<List<String>> out = new ArrayList<>();
        boolean header = true;
        for (String line : lines) {
            if (!Csv.isDataLine(line)) continue;
            if (header) { header = false; continue; }
            out.add(Csv.split(line).stream().map(String::trim).toList());
        }
        return out;
    }
}
