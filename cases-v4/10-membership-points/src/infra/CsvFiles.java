package infra;

import domain.Grade;
import domain.Member;
import domain.Payment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Fixture/export readers. Values never contain commas. */
public final class CsvFiles {
    private CsvFiles() {}

    public static List<Member> members(Path file) {
        List<Member> out = new ArrayList<>();
        for (String[] r : rows(file)) {
            out.add(new Member(cell(r[0]), r[1], Grade.valueOf(r[2]), r[3]));
        }
        return out;
    }

    public static List<Payment> payments(Path file) {
        List<Payment> out = new ArrayList<>();
        for (String[] r : rows(file)) {
            out.add(new Payment(r[0], cell(r[1]), r[2], Long.parseLong(r[3]), r[4]));
        }
        return out;
    }

    /**
     * The acquirer's payment export is made for spreadsheets: member numbers that start
     * with 0 are written as ="004512" so the spreadsheet keeps them as text.
     */
    static String cell(String raw) {
        String s = raw.strip();
        if (s.startsWith("=\"") && s.endsWith("\"") && s.length() >= 3) {
            return s.substring(2, s.length() - 1);
        }
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static List<String[]> rows(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<String[]> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String l = lines.get(i).strip();
                if (!l.isEmpty()) rows.add(l.split(",", -1));
            }
            return rows;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + file, e);
        }
    }
}
