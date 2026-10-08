import api.Response;
import app.MenuApp;
import db.Database;
import db.InMemoryDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HiddenMenuCheck {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "ok   - " : "FAIL - ") + msg);
        if (!cond) failed++;
    }

    /** Holds the chosen thread right before its first write (insert or update) to the given table. */
    static class WriteGate implements Database {
        final InMemoryDatabase delegate = new InMemoryDatabase();
        final AtomicBoolean fired = new AtomicBoolean();
        final CountDownLatch reached = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        volatile Thread target;
        volatile String table;

        public Map<String, Object> select(String t, String i) { return delegate.select(t, i); }
        public List<Map<String, Object>> selectAll(String t) { return delegate.selectAll(t); }
        public void insert(String t, String i, Map<String, Object> row) {
            gate(t);
            delegate.insert(t, i, row);
        }
        public long version() { return delegate.version(); }

        public void update(String t, String i, Map<String, Object> columns) {
            gate(t);
            delegate.update(t, i, columns);
        }

        private void gate(String t) {
            if (Thread.currentThread() == target && t.equals(table) && fired.compareAndSet(false, true)) {
                reached.countDown();
                try {
                    release.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    static String id(Response r) {
        Matcher m = Pattern.compile("\"id\":\"([^\"]*)\"").matcher(r.body());
        if (!m.find()) throw new IllegalStateException("no id: " + r);
        return m.group(1);
    }

    static List<String> names(MenuApp app, String cat) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("\"name\":\"([^\"]*)\"").matcher(app.handle("GET", "/menus/" + cat, null, Map.of()).body());
        while (m.find()) out.add(m.group(1));
        return out;
    }

    static String cat;
    static String[] ids;

    static MenuApp seed(Database db, String... names) {
        MenuApp app = MenuApp.create(db);
        cat = id(app.handle("POST", "/admin/categories", "ops", Map.of("title", "Desserts")));
        ids = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            ids[i] = id(app.handle("POST", "/admin/categories/" + cat + "/items", "ops", Map.of("name", names[i], "price", "5")));
        }
        return app;
    }

    /** Starts {@code paused} (gated), runs {@code other} to completion while it waits, then lets it continue. */
    static void race(WriteGate gate, Thread paused, Runnable other) throws Exception {
        gate.target = paused;
        paused.start();
        check(gate.reached.await(2, TimeUnit.SECONDS), "gate reached by " + paused.getName());
        Thread t = new Thread(other, "other");
        t.start();
        t.join(500);
        gate.release.countDown();
        paused.join(5000);
        t.join(5000);
    }

    public static void main(String[] args) throws Exception {
        // A: rename is about to write while a move to the top completes
        {
            WriteGate gate = new WriteGate();
            gate.table = "menu_item";
            MenuApp app = seed(gate, "Brownie", "Cheesecake", "Tiramisu", "Mochi");
            String mochi = ids[3];
            Thread renamer = new Thread(() -> app.handle("PATCH", "/admin/items/" + mochi, "a", Map.of("name", "Matcha Mochi")), "renamer");
            race(gate, renamer, () -> app.handle("POST", "/admin/items/" + mochi + "/move", "b", Map.of("index", "1")));
            List<String> b = names(app, cat);
            check(b.equals(List.of("Brownie", "Matcha Mochi", "Cheesecake", "Tiramisu")), "A: rename + move both kept " + b);
        }
        // B: a move is about to write while a rename of the moved item completes
        {
            WriteGate gate = new WriteGate();
            gate.table = "menu_item";
            MenuApp app = seed(gate, "Brownie", "Cheesecake", "Tiramisu");
            String tira = ids[2];
            Thread mover = new Thread(() -> app.handle("POST", "/admin/items/" + tira + "/move", "b", Map.of("index", "0")), "mover");
            race(gate, mover, () -> app.handle("PATCH", "/admin/items/" + tira, "a", Map.of("name", "Tiramisu Classico")));
            List<String> b = names(app, cat);
            check(b.equals(List.of("Tiramisu Classico", "Brownie", "Cheesecake")), "B: move + rename both kept " + b);
        }
        // C: adding a new item while someone reorders the existing ones
        {
            WriteGate gate = new WriteGate();
            gate.table = "menu_item";
            MenuApp app = seed(gate, "Brownie", "Cheesecake", "Tiramisu");
            String brownie = ids[0];
            Thread adder = new Thread(() -> app.handle("POST", "/admin/categories/" + cat + "/items", "a", Map.of("name", "Panna Cotta", "price", "6")), "adder");
            race(gate, adder, () -> app.handle("POST", "/admin/items/" + brownie + "/move", "b", Map.of("index", "2")));
            List<String> b = names(app, cat);
            check(b.size() == 4 && b.get(3).equals("Panna Cotta"), "C: new item added at the bottom " + b);
            check(b.subList(0, 3).equals(List.of("Cheesecake", "Tiramisu", "Brownie")), "C: reorder done meanwhile is kept " + b);
        }
        // D: price change and hiding the same item at the same time
        {
            WriteGate gate = new WriteGate();
            gate.table = "menu_item";
            MenuApp app = seed(gate, "Brownie", "Cheesecake");
            String cheese = ids[1];
            Thread pricer = new Thread(() -> app.handle("PATCH", "/admin/items/" + cheese, "a", Map.of("price", "7.25")), "pricer");
            race(gate, pricer, () -> app.handle("PATCH", "/admin/items/" + cheese, "b", Map.of("visible", "false")));
            check(names(app, cat).equals(List.of("Brownie")), "D: hidden item stays hidden " + names(app, cat));
            Map<String, Object> row = gate.select("menu_item", cheese);
            check(((Number) row.get("price_cents")).longValue() == 725, "D: new price stored " + row);
        }
        // E: sequential sanity
        {
            MenuApp app = seed(new InMemoryDatabase(), "One", "Two", "Three");
            app.handle("POST", "/admin/items/" + ids[0] + "/move", "a", Map.of("index", "99"));
            app.handle("PATCH", "/admin/items/" + ids[1], "a", Map.of("name", "Two"));
            app.handle("PATCH", "/admin/items/" + ids[2], "a", Map.of("name", "Drei", "price", "1.10"));
            check(names(app, cat).equals(List.of("Two", "Drei", "One")), "E: sequential " + names(app, cat));
            check(app.handle("PATCH", "/admin/items/" + ids[2], "a", Map.of()).status() == 400, "E: empty patch → 400");
        }

        if (failed > 0) {
            System.out.println(failed + " hidden check(s) failed");
            System.exit(1);
        }
        System.out.println("all hidden checks passed");
    }
}
