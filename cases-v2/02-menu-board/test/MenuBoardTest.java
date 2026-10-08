import api.Response;
import app.MenuApp;
import db.InMemoryDatabase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MenuBoardTest {
    static String drinks;
    static String americano;
    static String latte;
    static String icedTea;

    static void seed(MenuApp app) {
        drinks = TestSupport.id(app.handle("POST", "/admin/categories", "mia", Map.of("title", "Drinks")));
        americano = TestSupport.id(app.handle("POST", "/admin/categories/" + drinks + "/items", "mia", Map.of("name", "Americano", "price", "3.00")));
        latte = TestSupport.id(app.handle("POST", "/admin/categories/" + drinks + "/items", "mia", Map.of("name", "Latte", "price", "4.20")));
        icedTea = TestSupport.id(app.handle("POST", "/admin/categories/" + drinks + "/items", "mia", Map.of("name", "Iced Tea", "price", "3.50")));
    }

    public static void main(String[] args) throws Exception {
        // 1) one request after another
        {
            MenuApp app = MenuApp.create(new InMemoryDatabase());
            seed(app);
            TestSupport.check(TestSupport.names(app, drinks).equals(List.of("Americano", "Latte", "Iced Tea")), "new items go to the bottom");
            Response moved = app.handle("POST", "/admin/items/" + icedTea + "/move", "mia", Map.of("index", "0"));
            TestSupport.check(moved.status() == 204, "move accepted");
            app.handle("PATCH", "/admin/items/" + latte, "noah", Map.of("price", "4.40"));
            app.handle("PATCH", "/admin/items/" + icedTea, "noah", Map.of("name", "Peach Iced Tea"));
            TestSupport.check(TestSupport.names(app, drinks).equals(List.of("Peach Iced Tea", "Americano", "Latte")),
                    "sequential edits: " + TestSupport.names(app, drinks));
            TestSupport.check("$4.40".equals(TestSupport.price(app, drinks, "Latte")), "price change shows on the board");
            app.handle("PATCH", "/admin/items/" + americano, "noah", Map.of("visible", "false"));
            TestSupport.check(TestSupport.names(app, drinks).equals(List.of("Peach Iced Tea", "Latte")), "hidden items are not on the board");
            TestSupport.check(app.handle("POST", "/admin/items/nope/move", "mia", Map.of("index", "1")).status() == 404, "unknown item → 404");
            TestSupport.check(app.handle("PATCH", "/admin/items/" + latte, "mia", Map.of("name", "  ")).status() == 400, "blank name → 400");
        }

        // 2) two staff tablets at the same time: one renames a drink while the other drags it to the top
        {
            PausingDatabase db = new PausingDatabase(new InMemoryDatabase());
            MenuApp app = MenuApp.create(db);
            seed(app);

            Thread renamer = new Thread(() -> app.handle("PATCH", "/admin/items/" + icedTea, "noah", Map.of("name", "Cold Brew")), "tablet-1");
            db.pauseAfterRead(renamer, "menu_item", icedTea);
            renamer.start();
            db.reached.await(2, TimeUnit.SECONDS);

            Thread mover = new Thread(() -> app.handle("POST", "/admin/items/" + icedTea + "/move", "mia", Map.of("index", "0")), "tablet-2");
            mover.start();
            mover.join(500);
            db.release.countDown();
            renamer.join(5000);
            mover.join(5000);

            List<String> board = TestSupport.names(app, drinks);
            TestSupport.check(board.contains("Cold Brew"), "renamed drink shows its new name (board " + board + ")");
            TestSupport.check(board.equals(List.of("Cold Brew", "Americano", "Latte")),
                    "drink moved to the top stays on top after the rename (board " + board + ")");
        }

        TestSupport.finish("menu board");
    }
}
