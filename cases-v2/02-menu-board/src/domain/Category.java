package domain;

public class Category {
    private final String id;
    private String title;

    public Category(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public void retitle(String value) {
        this.title = value;
    }
}
