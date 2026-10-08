import db.InMemoryDatabase;
import domain.MenuItem;
import orm.Transactions;
import repo.MenuRepository;
import util.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MenuUnitChecks {
    public static void main(String[] args) {
        TestSupport.check(Money.format(450).equals("$4.50"), "money: format");
        TestSupport.check(Money.format(-5).equals("-$0.05"), "money: negative");
        TestSupport.check(Money.parse("$4.5") == 450 && Money.parse("12") == 1200 && Money.parse("0.07") == 7, "money: parse");

        InMemoryDatabase db = new InMemoryDatabase();
        db.insert("t", "a", Map.of("x", 1, "y", "one"));
        db.update("t", "a", Map.of("y", "two"));
        TestSupport.check(db.select("t", "a").equals(Map.of("id", "a", "x", 1, "y", "two")), "db: update");
        db.select("t", "a").put("x", 99);
        TestSupport.check(Integer.valueOf(1).equals(db.select("t", "a").get("x")), "db: returned rows are copies");
        long v = db.version();
        TestSupport.check(v == 2, "db: version counts writes");

        List<MenuItem> items = new ArrayList<>(List.of(
                new MenuItem("1", "c", "banana", 100, 20, true),
                new MenuItem("2", "c", "Apple", 100, 20, true),
                new MenuItem("3", "c", "cherry", 100, 10, true)));
        items.sort(MenuRepository.BOARD_ORDER);
        TestSupport.check(items.get(0).id().equals("3") && items.get(1).id().equals("2") && items.get(2).id().equals("1"),
                "board order: position then name, case-insensitive");

        InMemoryDatabase db2 = new InMemoryDatabase();
        Transactions tx = new Transactions(db2);
        MenuRepository repo = new MenuRepository();
        tx.run(s -> repo.add(s, new MenuItem("i1", "c", "Soup", 500, 10, true)));
        long before = db2.version();
        String name = tx.read(s -> {
            MenuItem it = repo.item(s, "i1");
            it.rename("changed only in memory");
            return repo.item(s, "i1").name();
        });
        TestSupport.check(name.equals("changed only in memory"), "session: identity map returns the same object");
        TestSupport.check(db2.version() == before && db2.select("menu_item", "i1").get("name").equals("Soup"), "read-only session writes nothing");

        TestSupport.finish("unit");
    }
}
