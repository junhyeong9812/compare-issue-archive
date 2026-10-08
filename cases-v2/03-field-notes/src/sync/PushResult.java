package sync;

import java.util.List;

public record PushResult(List<Item> items, long cursor) {
    public enum Status { SYNCED, REJECTED }

    public record Item(String noteId, Status status, String reason) {}
}
