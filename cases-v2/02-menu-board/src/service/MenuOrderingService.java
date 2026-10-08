package service;

import domain.MenuItem;
import orm.Transactions;
import repo.MenuRepository;
import util.AuditTrail;

import java.util.List;

/** Drag-and-drop ordering on the menu board. Positions are spaced so single inserts rarely renumber. */
public class MenuOrderingService {
    static final int STEP = 10;

    private final Transactions tx;
    private final MenuRepository repo;
    private final AuditTrail audit;

    public MenuOrderingService(Transactions tx, MenuRepository repo, AuditTrail audit) {
        this.tx = tx;
        this.repo = repo;
        this.audit = audit;
    }

    /** Moves the item so it ends up at {@code index} (0 = top) within its category. */
    public void move(String actor, String itemId, int index) {
        tx.run(s -> {
            MenuItem target = repo.item(s, itemId);
            if (target == null) {
                throw ApiException.notFound("item " + itemId);
            }
            List<MenuItem> items = repo.itemsOf(s, target.categoryId());
            items.remove(target);
            int at = Math.max(0, Math.min(index, items.size()));
            items.add(at, target);
            renumber(items);
            s.afterCommit(() -> audit.record(actor, "moved " + itemId + " to #" + at));
        });
    }

    static void renumber(List<MenuItem> items) {
        int position = STEP;
        for (MenuItem item : items) {
            if (item.position() != position) {
                item.moveTo(position);
            }
            position += STEP;
        }
    }
}
