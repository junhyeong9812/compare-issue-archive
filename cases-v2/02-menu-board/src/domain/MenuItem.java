package domain;

/** A dish or drink on a menu board. Mutable: loaded and changed inside a session. */
public class MenuItem {
    private final String id;
    private final String categoryId;
    private String name;
    private long priceCents;
    private int position;
    private boolean visible;

    public MenuItem(String id, String categoryId, String name, long priceCents, int position, boolean visible) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.priceCents = priceCents;
        this.position = position;
        this.visible = visible;
    }

    public String id() {
        return id;
    }

    public String categoryId() {
        return categoryId;
    }

    public String name() {
        return name;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public long priceCents() {
        return priceCents;
    }

    public void reprice(long cents) {
        this.priceCents = cents;
    }

    public int position() {
        return position;
    }

    public void moveTo(int newPosition) {
        this.position = newPosition;
    }

    public boolean visible() {
        return visible;
    }

    public void setVisible(boolean value) {
        this.visible = value;
    }

    @Override
    public String toString() {
        return "MenuItem[" + id + " '" + name + "' pos=" + position + " price=" + priceCents + (visible ? "" : " hidden") + "]";
    }
}
