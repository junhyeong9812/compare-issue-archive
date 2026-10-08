package api;

import domain.Note;
import util.Timestamps;

final class Json {
    private Json() {
    }

    static String str(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    static String note(Note n) {
        return "{\"id\":" + str(n.id())
                + ",\"folder\":" + str(n.folderId())
                + ",\"title\":" + str(n.title())
                + ",\"body\":" + str(n.body())
                + ",\"updatedAt\":" + str(Timestamps.format(n.updatedAt()))
                + ",\"revision\":" + n.revision()
                + ",\"deleted\":" + n.deleted() + "}";
    }
}
