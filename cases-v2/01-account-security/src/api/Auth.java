package api;

import service.ValidationException;

final class Auth {
    private Auth() {
    }

    static String bearer(Request req) {
        String h = req.header("Authorization");
        if (h == null || !h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw ValidationException.of(401, "not signed in");
        }
        return h.substring(7).trim();
    }
}
