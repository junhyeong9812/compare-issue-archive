package repo;

import domain.Category;
import orm.EntityMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class CategoryMapper implements EntityMapper<Category> {
    public static final CategoryMapper INSTANCE = new CategoryMapper();

    @Override
    public String table() {
        return "category";
    }

    @Override
    public String idOf(Category entity) {
        return entity.id();
    }

    @Override
    public Map<String, Object> toRow(Category e) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("title", e.title());
        return row;
    }

    @Override
    public Category fromRow(Map<String, Object> row) {
        return new Category((String) row.get("id"), (String) row.get("title"));
    }
}
