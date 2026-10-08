package orm;

import db.Database;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Unit of work. Entities loaded through a session are tracked; {@link #flush()} writes them back,
 * so services only change objects and never call update themselves.
 */
public class Session {
    private record Managed(EntityMapper<Object> mapper, Object entity) {}

    private final Database db;
    private final Map<String, Managed> identityMap = new LinkedHashMap<>();
    private final List<Runnable> afterCommit = new ArrayList<>();

    Session(Database db) {
        this.db = db;
    }

    public <T> T find(EntityMapper<T> mapper, String id) {
        String key = mapper.table() + "/" + id;
        Managed managed = identityMap.get(key);
        if (managed != null) {
            return cast(managed.entity());
        }
        Map<String, Object> row = db.select(mapper.table(), id);
        if (row == null) {
            return null;
        }
        return track(mapper, mapper.fromRow(row));
    }

    public <T> List<T> findWhere(EntityMapper<T> mapper, Predicate<Map<String, Object>> filter) {
        List<T> result = new ArrayList<>();
        for (Map<String, Object> row : db.selectAll(mapper.table())) {
            if (!filter.test(row)) {
                continue;
            }
            String key = mapper.table() + "/" + row.get("id");
            Managed managed = identityMap.get(key);
            result.add(managed != null ? cast(managed.entity()) : track(mapper, mapper.fromRow(row)));
        }
        return result;
    }

    public <T> void add(EntityMapper<T> mapper, T entity) {
        db.insert(mapper.table(), mapper.idOf(entity), mapper.toRow(entity));
        track(mapper, entity);
    }

    public void afterCommit(Runnable action) {
        afterCommit.add(action);
    }

    void flush() {
        for (Managed m : identityMap.values()) {
            db.update(m.mapper().table(), m.mapper().idOf(m.entity()), m.mapper().toRow(m.entity()));
        }
    }

    void runAfterCommit() {
        for (Runnable r : afterCommit) {
            r.run();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T track(EntityMapper<T> mapper, T entity) {
        identityMap.put(mapper.table() + "/" + mapper.idOf(entity), new Managed((EntityMapper<Object>) mapper, entity));
        return entity;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }
}
