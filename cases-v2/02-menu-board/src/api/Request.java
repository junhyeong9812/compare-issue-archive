package api;

import java.util.Map;

public record Request(String method, String path, Map<String, String> headers, Map<String, String> params) {
    public Request {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String name) {
        return params.get(name);
    }

    public String staff() {
        String s = headers.get("X-Staff");
        return s == null || s.isBlank() ? "unknown" : s;
    }
}
