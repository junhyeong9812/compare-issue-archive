package util;

import java.util.ArrayList;
import java.util.List;

/** Who changed what on the menu, for the back-office history page. */
public class AuditTrail {
    private final List<String> entries = new ArrayList<>();

    public synchronized void record(String actor, String action) {
        entries.add(actor + ": " + action);
    }

    public synchronized List<String> entries() {
        return List.copyOf(entries);
    }
}
