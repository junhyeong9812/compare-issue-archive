package api;

import service.ApiException;

public class Router {
    private final PublicMenuHandler publicMenu;
    private final AdminMenuHandler admin;

    public Router(PublicMenuHandler publicMenu, AdminMenuHandler admin) {
        this.publicMenu = publicMenu;
        this.admin = admin;
    }

    public Response handle(Request req) {
        try {
            return dispatch(req);
        } catch (ApiException e) {
            return new Response(e.status(), "{\"error\":" + BoardJson.str(e.getMessage()) + "}");
        } catch (RuntimeException e) {
            return new Response(500, "{\"error\":\"internal error\"}");
        }
    }

    private Response dispatch(Request req) {
        String[] p = req.path().split("/");
        String m = req.method();
        // p[0] is "" because paths start with '/'
        if (m.equals("GET") && p.length == 3 && p[1].equals("menus")) {
            return publicMenu.board(p[2]);
        }
        if (p.length >= 3 && p[1].equals("admin")) {
            if (m.equals("POST") && p.length == 3 && p[2].equals("categories")) {
                return admin.createCategory(req);
            }
            if (m.equals("POST") && p.length == 5 && p[2].equals("categories") && p[4].equals("items")) {
                return admin.addItem(req, p[3]);
            }
            if (m.equals("PATCH") && p.length == 4 && p[2].equals("items")) {
                return admin.patchItem(req, p[3]);
            }
            if (m.equals("POST") && p.length == 5 && p[2].equals("items") && p[4].equals("move")) {
                return admin.moveItem(req, p[3]);
            }
        }
        return new Response(404, "{\"error\":\"no route\"}");
    }
}
