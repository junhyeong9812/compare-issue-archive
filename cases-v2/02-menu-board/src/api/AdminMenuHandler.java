package api;

import service.ApiException;
import service.CategoryService;
import service.MenuEditService;
import service.MenuOrderingService;
import util.Money;

/** Back-office endpoints used by the staff tablet app. */
public class AdminMenuHandler {
    private final CategoryService categories;
    private final MenuEditService edits;
    private final MenuOrderingService ordering;

    public AdminMenuHandler(CategoryService categories, MenuEditService edits, MenuOrderingService ordering) {
        this.categories = categories;
        this.edits = edits;
        this.ordering = ordering;
    }

    Response createCategory(Request req) {
        String id = categories.create(req.param("title"));
        return new Response(201, "{\"id\":" + BoardJson.str(id) + "}");
    }

    Response addItem(Request req, String categoryId) {
        String price = req.param("price");
        if (price == null) {
            throw ApiException.badRequest("price required");
        }
        String id = edits.addItem(req.staff(), categoryId, req.param("name"), parsePrice(price));
        return new Response(201, "{\"id\":" + BoardJson.str(id) + "}");
    }

    Response patchItem(Request req, String itemId) {
        String price = req.param("price");
        String visible = req.param("visible");
        MenuEditService.ItemChange change = new MenuEditService.ItemChange(
                req.param("name"),
                price == null ? null : parsePrice(price),
                visible == null ? null : Boolean.valueOf(visible));
        edits.change(req.staff(), itemId, change);
        return new Response(204, "");
    }

    Response moveItem(Request req, String itemId) {
        String index = req.param("index");
        if (index == null || !index.matches("\\d{1,4}")) {
            throw ApiException.badRequest("index must be a non-negative number");
        }
        ordering.move(req.staff(), itemId, Integer.parseInt(index));
        return new Response(204, "");
    }

    private static long parsePrice(String text) {
        try {
            return Money.parse(text);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("bad price: " + text);
        }
    }
}
