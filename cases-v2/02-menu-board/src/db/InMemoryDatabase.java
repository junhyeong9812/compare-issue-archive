package db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryDatabase implements Database {
    private final Map<String, Map<String, Map<String, Object>>> tables = new HashMap<>();
    private long version = 0;

    @Override
    public synchronized Map<String, Object> select(String table, String id) {
        Map<String, Object> row = rows(table).get(id);
        return row == null ? null : new LinkedHashMap<>(row);
    }

    @Override
    public synchronized List<Map<String, Object>> selectAll(String table) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows(table).values()) {
            result.add(new LinkedHashMap<>(row));
        }
        return result;
    }

    @Override
    public synchronized void insert(String table, String id, Map<String, Object> row) {
        if (rows(table).containsKey(id)) {
            throw new IllegalStateException("duplicate key " + table + "/" + id);
        }
        Map<String, Object> copy = new LinkedHashMap<>(row);
        copy.put("id", id);
        rows(table).put(id, copy);
        version++;
    }

    @Override
    public synchronized void update(String table, String id, Map<String, Object> columns) {
        Map<String, Object> existing = rows(table).get(id);
        if (existing == null) {
            throw new IllegalStateException("no row " + table + "/" + id);
        }
        Map<String, Object> next = new LinkedHashMap<>(existing);
        next.putAll(columns);
        next.put("id", id);
        rows(table).put(id, next);
        version++;
    }

    @Override
    public synchronized long version() {
        return version;
    }

    private Map<String, Map<String, Object>> rows(String table) {
        return tables.computeIfAbsent(table, t -> new LinkedHashMap<>());
    }
}
