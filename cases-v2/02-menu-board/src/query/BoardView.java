package query;

import java.util.List;

public record BoardView(String categoryId, String title, List<Entry> entries) {
    public record Entry(String id, String name, String price) {}
}
