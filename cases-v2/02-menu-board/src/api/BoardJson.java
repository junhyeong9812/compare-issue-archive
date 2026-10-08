package api;

import query.BoardView;

final class BoardJson {
    private BoardJson() {
    }

    static String render(BoardView view) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"category\":").append(str(view.categoryId()))
                .append(",\"title\":").append(str(view.title()))
                .append(",\"items\":[");
        for (int i = 0; i < view.entries().size(); i++) {
            BoardView.Entry e = view.entries().get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(str(e.id()))
                    .append(",\"name\":").append(str(e.name()))
                    .append(",\"price\":").append(str(e.price())).append('}');
        }
        return sb.append("]}").toString();
    }

    static String str(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
