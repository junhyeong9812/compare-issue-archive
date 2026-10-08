package orm;

import java.util.Map;

/** Converts between an entity object and its table row. */
public interface EntityMapper<T> {
    String table();

    String idOf(T entity);

    Map<String, Object> toRow(T entity);

    T fromRow(Map<String, Object> row);
}
