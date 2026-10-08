package query;

import db.Database;
import domain.Category;
import domain.MenuItem;
import orm.Transactions;
import repo.MenuRepository;
import util.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Public menu board. Cached per category until the database changes. */
public class MenuQueryService {
    private record Cached(long version, BoardView view) {}

    private final Transactions tx;
    private final MenuRepository repo;
    private final Database db;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public MenuQueryService(Transactions tx, MenuRepository repo, Database db) {
        this.tx = tx;
        this.repo = repo;
        this.db = db;
    }

    public BoardView board(String categoryId) {
        long version = db.version();
        Cached cached = cache.get(categoryId);
        if (cached != null && cached.version() == version) {
            return cached.view();
        }
        BoardView view = tx.read(s -> build(s, categoryId));
        if (view != null) {
            cache.put(categoryId, new Cached(version, view));
        }
        return view;
    }

    private BoardView build(orm.Session s, String categoryId) {
        Category category = repo.category(s, categoryId);
        if (category == null) {
            return null;
        }
        List<BoardView.Entry> entries = new ArrayList<>();
        for (MenuItem item : repo.itemsOf(s, categoryId)) {
            if (item.visible()) {
                entries.add(new BoardView.Entry(item.id(), item.name(), Money.format(item.priceCents())));
            }
        }
        return new BoardView(categoryId, category.title(), List.copyOf(entries));
    }
}
