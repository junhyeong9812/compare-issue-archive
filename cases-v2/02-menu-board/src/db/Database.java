package db;

import java.util.List;
import java.util.Map;

/** Row storage. Rows are column-name to value maps; every call returns copies. */
public interface Database {
    Map<String, Object> select(String table, String id);

    List<Map<String, Object>> selectAll(String table);

    void insert(String table, String id, Map<String, Object> row);

    /** Sets the given columns on an existing row; columns not mentioned keep their value. */
    void update(String table, String id, Map<String, Object> columns);

    /** Increases on every write; readers use it to detect changes. */
    long version();
}
