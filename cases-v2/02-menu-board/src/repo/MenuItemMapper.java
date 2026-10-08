package repo;

import domain.MenuItem;
import orm.EntityMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class MenuItemMapper implements EntityMapper<MenuItem> {
    public static final MenuItemMapper INSTANCE = new MenuItemMapper();

    @Override
    public String table() {
        return "menu_item";
    }

    @Override
    public String idOf(MenuItem entity) {
        return entity.id();
    }

    @Override
    public Map<String, Object> toRow(MenuItem e) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("category_id", e.categoryId());
        row.put("name", e.name());
        row.put("price_cents", e.priceCents());
        row.put("position", e.position());
        row.put("visible", e.visible());
        return row;
    }

    @Override
    public MenuItem fromRow(Map<String, Object> row) {
        return new MenuItem(
                (String) row.get("id"),
                (String) row.get("category_id"),
                (String) row.get("name"),
                ((Number) row.get("price_cents")).longValue(),
                ((Number) row.get("position")).intValue(),
                Boolean.TRUE.equals(row.get("visible")));
    }
}
