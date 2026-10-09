package infra;

import domain.CatalogItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads fixtures/catalog.csv (sku,name,price — no quoting, names never contain commas). */
public final class CatalogLoader {
    private CatalogLoader() {}

    public static Map<String, CatalogItem> load(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + file, e);
        }
        Map<String, CatalogItem> items = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String l = lines.get(i).strip();
            if (l.isEmpty()) continue;
            String[] r = l.split(",", -1);
            items.put(r[0], new CatalogItem(r[0], r[1], Long.parseLong(r[2])));
        }
        return items;
    }
}
