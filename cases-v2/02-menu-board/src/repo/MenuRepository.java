package repo;

import domain.Category;
import domain.MenuItem;
import orm.Session;

import java.util.Comparator;
import java.util.List;

public class MenuRepository {
    /** Board order: position, then name so equal positions still list deterministically. */
    public static final Comparator<MenuItem> BOARD_ORDER =
            Comparator.comparingInt(MenuItem::position).thenComparing(MenuItem::name, String.CASE_INSENSITIVE_ORDER);

    public MenuItem item(Session session, String id) {
        return session.find(MenuItemMapper.INSTANCE, id);
    }

    public List<MenuItem> itemsOf(Session session, String categoryId) {
        List<MenuItem> items = session.findWhere(MenuItemMapper.INSTANCE, row -> categoryId.equals(row.get("category_id")));
        items.sort(BOARD_ORDER);
        return items;
    }

    public Category category(Session session, String id) {
        return session.find(CategoryMapper.INSTANCE, id);
    }

    public List<Category> categories(Session session) {
        return session.findWhere(CategoryMapper.INSTANCE, row -> true);
    }

    public void add(Session session, MenuItem item) {
        session.add(MenuItemMapper.INSTANCE, item);
    }

    public void add(Session session, Category category) {
        session.add(CategoryMapper.INSTANCE, category);
    }
}
