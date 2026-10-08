package api;

import query.BoardView;
import query.MenuQueryService;
import service.ApiException;

/** GET /menus/{category} — what guests see on the screen and the web site. */
public class PublicMenuHandler {
    private final MenuQueryService queries;

    public PublicMenuHandler(MenuQueryService queries) {
        this.queries = queries;
    }

    Response board(String categoryId) {
        BoardView view = queries.board(categoryId);
        if (view == null) {
            throw ApiException.notFound("category " + categoryId);
        }
        return new Response(200, BoardJson.render(view));
    }
}
