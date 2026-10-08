package service;

import domain.Category;
import domain.MenuItem;
import orm.Transactions;
import repo.MenuRepository;
import util.AuditTrail;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Back-office edits of single menu items: create, rename, price, visibility. */
public class MenuEditService {
    public record ItemChange(String name, Long priceCents, Boolean visible) {
        public boolean isEmpty() {
            return name == null && priceCents == null && visible == null;
        }
    }

    private static final int MAX_NAME = 60;

    private final Transactions tx;
    private final MenuRepository repo;
    private final AuditTrail audit;
    private final AtomicLong ids = new AtomicLong();

    public MenuEditService(Transactions tx, MenuRepository repo, AuditTrail audit) {
        this.tx = tx;
        this.repo = repo;
        this.audit = audit;
    }

    public String addItem(String actor, String categoryId, String name, long priceCents) {
        String clean = cleanName(name);
        if (priceCents < 0) {
            throw ApiException.badRequest("price must not be negative");
        }
        return tx.call(s -> {
            Category category = repo.category(s, categoryId);
            if (category == null) {
                throw ApiException.notFound("category " + categoryId);
            }
            List<MenuItem> existing = repo.itemsOf(s, categoryId);
            int position = existing.isEmpty() ? MenuOrderingService.STEP
                    : existing.get(existing.size() - 1).position() + MenuOrderingService.STEP;
            String id = "item-" + ids.incrementAndGet();
            repo.add(s, new MenuItem(id, categoryId, clean, priceCents, position, true));
            s.afterCommit(() -> audit.record(actor, "added " + id + " '" + clean + "'"));
            return id;
        });
    }

    public void change(String actor, String itemId, ItemChange change) {
        if (change.isEmpty()) {
            throw ApiException.badRequest("nothing to change");
        }
        String newName = change.name() == null ? null : cleanName(change.name());
        if (change.priceCents() != null && change.priceCents() < 0) {
            throw ApiException.badRequest("price must not be negative");
        }
        tx.run(s -> {
            MenuItem item = repo.item(s, itemId);
            if (item == null) {
                throw ApiException.notFound("item " + itemId);
            }
            if (newName != null) {
                item.rename(newName);
            }
            if (change.priceCents() != null) {
                item.reprice(change.priceCents());
            }
            if (change.visible() != null) {
                item.setVisible(change.visible());
            }
            s.afterCommit(() -> audit.record(actor, "changed " + itemId + " " + change));
        });
    }

    private static String cleanName(String name) {
        String s = name == null ? "" : name.strip().replaceAll("\\s+", " ");
        if (s.isEmpty() || s.length() > MAX_NAME) {
            throw ApiException.badRequest("name must be 1-" + MAX_NAME + " characters");
        }
        return s;
    }
}
